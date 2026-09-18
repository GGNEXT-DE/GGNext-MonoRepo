package eu.ggnext.railway.profile

import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import eu.ggnext.common.db.MongoManager
import eu.ggnext.common.logging.LogControl
import eu.ggnext.contentsystem.value.store.NumberStore
import eu.ggnext.contentsystem.value.types.Quest
import eu.ggnext.contentsystem.value.types.QuestTrackingType
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RailwayProfileManager(
    private val mongoManager: MongoManager,
) {
    private val profileCollection = mongoManager.database.getCollection<RailwayProfile>("railway_profiles")
    private val profileIndexCollection = mongoManager.database.getCollection<RailwayProfileIndex>("railway_profile_index")
    private val maxRailwayAccounts by NumberStore("numbers.railway.profile.max_railway_accounts")
    private val defaultRailwayDollars by NumberStore("numbers.railway.profile.default_railway_dollars")
    private val maxNameLength by NumberStore("numbers.railway.profile.max_name_length")

    private val activeProfiles = ConcurrentHashMap<UUID, UUID>()
    private val logger = LogControl.logger(this::class.java)

    suspend fun createProfile(
        player: Player,
        name: String,
    ): RailwayProfile? {
        val indexedProfileAmount =
            profileIndexCollection
                .find(Filters.eq("_id", player.uniqueId))
                .firstOrNull()
                ?.profileIds
                ?.size
                ?: 0
        val currentProfileAmount = getProfiles(player).size

        logger.info(
            "Creating Railway profile for ${player.name} (${player.uniqueId}): " +
                "name='$name', nameLength=${name.length}, indexedProfiles=$indexedProfileAmount, " +
                "existingProfiles=$currentProfileAmount, " +
                "maxProfiles=$maxRailwayAccounts, maxNameLength=$maxNameLength",
        )

        if (currentProfileAmount >= maxRailwayAccounts) {
            logger.info("Railway profile creation rejected for ${player.name}: profile limit reached")
            return null
        }
        if (name.length > maxNameLength) {
            logger.info("Railway profile creation rejected for ${player.name}: name exceeds maximum length")
            return null
        }

        val profileId = UUID.randomUUID()
        val railwayProfile =
            RailwayProfile(
                profileId,
                railwayDollars = defaultRailwayDollars.toDouble(),
                name = name,
                level = RailwayLevel(0L, 0),
                completedQuests = emptySet(),
                activeQuests = emptySet(),
            )

        profileCollection.insertOne(railwayProfile)
        logger.info("Inserted Railway profile $profileId for ${player.name}")
        profileIndexCollection.updateOne(
            Filters.eq("_id", player.uniqueId),
            Updates.addToSet("profileIds", profileId),
            UpdateOptions().upsert(true),
        )
        logger.info("Added Railway profile $profileId to the profile index for ${player.name}")
        return railwayProfile
    }

    suspend fun updateProfile(profile: RailwayProfile) = profileCollection.replaceOne(Filters.eq("_id", profile.id), profile)

    suspend fun renameActiveProfile(
        player: Player,
        newName: String,
    ): RailwayProfile? {
        if (newName.length > maxNameLength) return null

        val activeProfileId = getActiveProfileId(player) ?: return null
        val profile = getProfile(activeProfileId) ?: return null

        val renamedProfile = profile.copy(name = newName)
        updateProfile(renamedProfile)
        return renamedProfile
    }
    suspend fun unlockSkillTier(
        profile: RailwayProfile,
        cost: Int,
        pathId: String,
        newTierCount: Int,
    ): Boolean =
        profileCollection
            .updateOne(
                Filters.and(Filters.eq("_id", profile.id), Filters.gte("level.skillPoints", cost)),
                Updates.combine(
                    Updates.inc("level.skillPoints", -cost),
                    Updates.set("level.unlockedTiers.$pathId", newTierCount),
                ),
            ).modifiedCount == 1L

    suspend fun getProfile(profileId: UUID): RailwayProfile? = profileCollection.find(Filters.eq("_id", profileId)).firstOrNull()

    suspend fun getProfilesByIds(profileIds: Collection<UUID>): Map<UUID, RailwayProfile> {
        if (profileIds.isEmpty()) return emptyMap()
        return profileCollection
            .find(Filters.`in`("_id", profileIds))
            .toList()
            .associateBy { it.id }
    }

    suspend fun getProfiles(player: Player): List<RailwayProfile> {
        val profileIds =
            profileIndexCollection
                .find(Filters.eq("_id", player.uniqueId))
                .firstOrNull()
                ?.profileIds
                ?: return emptyList()

        if (profileIds.isEmpty()) return emptyList()

        return profileCollection
            .find(Filters.`in`("_id", profileIds))
            .toList()
    }

    suspend fun getProfileByName(
        player: Player,
        name: String,
    ): RailwayProfile? {
        val profiles = getProfiles(player)
        if (profiles.isEmpty()) return null

        profiles.firstOrNull { it.name == name }?.let { return it }
        return null
    }

    suspend fun deleteProfile(
        player: Player,
        profileId: UUID,
    ) {
        if (getActiveProfileId(player) == profileId) {
            return
        }
        profileCollection.deleteOne(Filters.eq("_id", profileId))
        profileIndexCollection.updateOne(
            Filters.eq("_id", player.uniqueId),
            Updates.pull("profileIds", profileId),
        )
    }

    fun setActiveProfile(
        player: Player,
        activeProfile: RailwayProfile,
    ) {
        activeProfiles[player.uniqueId] = activeProfile.id
    }

    fun getActiveProfileId(player: Player): UUID? = activeProfiles[player.uniqueId]

    fun deleteActiveProfile(player: Player) = activeProfiles.remove(player.uniqueId)

    suspend fun addDollars(
        profile: RailwayProfile,
        amount: Double,
    ) = profileCollection.updateOne(Filters.eq("_id", profile.id), Updates.inc("railwayDollars", amount))

    suspend fun removeDollars(
        profile: RailwayProfile,
        amount: Double,
    ): Boolean =
        profileCollection
            .updateOne(
                Filters.and(Filters.eq("_id", profile.id), Filters.gte("railwayDollars", amount)),
                Updates.inc("railwayDollars", -amount),
            ).modifiedCount == 1L

    suspend fun addXp(
        profile: RailwayProfile,
        amount: Long,
    ) {
        val oldLevel = profile.level.currentLevel

        val newXp = profile.level.xp + amount
        val newLevelObj = RailwayLevel(newXp, profile.level.skillPoints)
        val newLevel = newLevelObj.currentLevel

        val levelGain = newLevel - oldLevel

        val updates =
            mutableListOf(
                Updates.inc("level.xp", amount),
            )

        if (levelGain > 0) {
            val earnedSkillPoints = levelGain * 1
            updates.add(Updates.inc("level.skillPoints", earnedSkillPoints))
        }

        profileCollection.updateOne(
            Filters.eq("_id", profile.id),
            Updates.combine(updates),
        )
    }

    suspend fun removeSkillPoints(
        profile: RailwayProfile,
        amount: Int,
    ): Boolean =
        profileCollection
            .updateOne(
                Filters.and(Filters.eq("_id", profile.id), Filters.gte("level.skillPoints", amount)),
                Updates.inc("level.skillPoints", -amount),
            ).modifiedCount == 1L

    suspend fun completeQuest(
        profile: RailwayProfile,
        questId: String,
    ) {
        profileCollection.updateOne(
            Filters.eq("_id", profile.id),
            Updates.combine(
                Updates.addToSet("completedQuests", questId),
                Updates.pullByFilter(Filters.eq("activeQuests", Filters.eq("questId", questId))),
            ),
        )
    }

    suspend fun addActiveQuest(
        profile: RailwayProfile,
        questId: String,
        type: QuestTrackingType,
    ) {
        val questProgress = QuestProgress(questId, type)
        profileCollection.updateOne(Filters.eq("_id", profile.id), Updates.addToSet("activeQuests", questProgress))
    }

    suspend fun getActiveQuests(profile: RailwayProfile): Set<QuestProgress> =
        profileCollection.find(Filters.eq("_id", profile.id)).firstOrNull()?.activeQuests ?: emptySet()

    suspend fun updateActiveQuest(
        profile: RailwayProfile,
        questId: String,
        questProgress: QuestProgress,
    ): Boolean {
        val result =
            profileCollection.updateOne(
                Filters.and(Filters.eq("_id", profile.id), Filters.eq("activeQuests.questId", questId)),
                Updates.set("activeQuests.$.currentValue", questProgress.currentValue),
            )
        return result.modifiedCount > 0
    }
}
