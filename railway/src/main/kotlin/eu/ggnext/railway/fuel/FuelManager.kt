package eu.ggnext.railway.fuel

import eu.ggnext.contentsystem.value.store.NumberStore
import eu.ggnext.contentsystem.value.store.SkillPathStore
import eu.ggnext.contentsystem.value.types.EffectType
import eu.ggnext.railway.profile.RailwayProfile
import eu.ggnext.railway.profile.RailwayProfileManager
import eu.ggnext.railway.skilltree.effect.EffectManager
import kotlin.math.floor
import kotlin.math.min

class FuelManager(
    private val profileManager: RailwayProfileManager,
    private val effectManager: EffectManager,
) {
    private val baseMaxFuel by NumberStore("numbers.railway.fuel.base_max_fuel")
    private val baseRegenSeconds by NumberStore("numbers.railway.fuel.base_regen_seconds")
    private val zoneTravelCost by NumberStore("numbers.railway.fuel.zone_travel_cost")
    private val baseAfterburnerCooldownMinutes by NumberStore("numbers.railway.fuel.afterburner.base_cooldown_minutes")
    private val afterburnerFuelPerCharge by NumberStore("numbers.railway.fuel.afterburner.fuel_per_charge")
    private val afterburnerMinutesPerCharge by NumberStore("numbers.railway.fuel.afterburner.minutes_per_charge")

    private fun effects(profile: RailwayProfile): Map<EffectType, Double> =
        effectManager.calculateEffects(SkillPathStore.getAllSkillPaths(), profile.level.unlockedTiers)

    fun effectiveMaxFuel(profile: RailwayProfile): Double = baseMaxFuel + (effects(profile)[EffectType.MAX_FUEL] ?: 0.0)

    fun effectiveRegenSeconds(profile: RailwayProfile): Double = baseRegenSeconds + (effects(profile)[EffectType.FUEL_REGEN_SECONDS] ?: 0.0)

    fun isAfterburnerUnlocked(profile: RailwayProfile): Boolean = (profile.level.unlockedTiers["nachbrenner"] ?: 0) > 0

    fun afterburnerCooldownMinutes(profile: RailwayProfile): Int? {
        if (!isAfterburnerUnlocked(profile)) return null
        val delta = effects(profile)[EffectType.AFTERBURNER_COOLDOWN_MINUTES] ?: 0.0
        return (baseAfterburnerCooldownMinutes + delta).toInt()
    }

    fun currentFuel(profile: RailwayProfile): Double {
        val now = System.currentTimeMillis()
        val cooldownUntil = profile.fuel.afterburner.cooldownUntil
        val regenApplies = cooldownUntil == null || now >= cooldownUntil
        val elapsedSeconds = (now - profile.fuel.lastUpdate) / 1000.0
        val regenerated = if (regenApplies) elapsedSeconds / effectiveRegenSeconds(profile) else 0.0
        return min(effectiveMaxFuel(profile), profile.fuel.amount + regenerated)
    }

    suspend fun consumeForZoneEntry(profile: RailwayProfile): FuelConsumeResult {
        val now = System.currentTimeMillis()
        val afterburner = profile.fuel.afterburner
        if (afterburner.chargesRemaining > 0 && afterburner.windowEndsAt != null && now < afterburner.windowEndsAt) {
            return if (profileManager.consumeAfterburnerCharge(profile)) {
                FuelConsumeResult.UsedAfterburnerCharge(afterburner.chargesRemaining - 1)
            } else {
                FuelConsumeResult.InsufficientFuel(currentFuel(profile), zoneTravelCost)
            }
        }

        val current = currentFuel(profile)
        if (current < zoneTravelCost) return FuelConsumeResult.InsufficientFuel(current, zoneTravelCost)

        return if (profileManager.consumeFuel(profile, current, zoneTravelCost, now)) {
            FuelConsumeResult.Success(current - zoneTravelCost)
        } else {
            FuelConsumeResult.InsufficientFuel(current, zoneTravelCost)
        }
    }

    suspend fun addFuel(
        profile: RailwayProfile,
        amount: Double,
    ): Boolean {
        val now = System.currentTimeMillis()
        val current = currentFuel(profile)
        return profileManager.addFuel(profile, current, amount, effectiveMaxFuel(profile), now)
    }

    suspend fun canActivateAfterburner(profile: RailwayProfile): Boolean = previewAfterburner(profile) != null

    /** Computes what activating the afterburner right now would grant, without mutating anything. */
    fun previewAfterburner(profile: RailwayProfile): AfterburnerPreview? {
        val cooldownMinutes = afterburnerCooldownMinutes(profile) ?: return null
        val charges = floor(currentFuel(profile) / afterburnerFuelPerCharge).toInt()
        if (charges <= 0) return null
        val windowMinutes = charges * afterburnerMinutesPerCharge
        return AfterburnerPreview(charges, windowMinutes, cooldownMinutes)
    }

    suspend fun activateAfterburner(profile: RailwayProfile): AfterburnerActivationResult {
        if (!isAfterburnerUnlocked(profile)) return AfterburnerActivationResult.NotUnlocked
        val preview = previewAfterburner(profile) ?: return AfterburnerActivationResult.NoFuel

        val now = System.currentTimeMillis()
        val windowEndsAt = now + preview.windowMinutes * 60_000L
        val cooldownUntil = windowEndsAt + preview.cooldownMinutes * 60_000L

        profileManager.activateAfterburner(profile, preview.charges, windowEndsAt, cooldownUntil)
        return AfterburnerActivationResult.Success(preview.charges, preview.windowMinutes, preview.cooldownMinutes)
    }
}

data class AfterburnerPreview(
    val charges: Int,
    val windowMinutes: Int,
    val cooldownMinutes: Int,
)

sealed interface FuelConsumeResult {
    data class Success(
        val remainingFuel: Double,
    ) : FuelConsumeResult

    data class UsedAfterburnerCharge(
        val chargesRemaining: Int,
    ) : FuelConsumeResult

    data class InsufficientFuel(
        val currentFuel: Double,
        val required: Int,
    ) : FuelConsumeResult
}

sealed interface AfterburnerActivationResult {
    data class Success(
        val charges: Int,
        val windowMinutes: Int,
        val cooldownMinutes: Int,
    ) : AfterburnerActivationResult

    data object NotUnlocked : AfterburnerActivationResult

    data object NoFuel : AfterburnerActivationResult
}
