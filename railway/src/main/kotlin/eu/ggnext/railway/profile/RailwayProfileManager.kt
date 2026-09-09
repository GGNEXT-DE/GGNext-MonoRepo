package eu.ggnext.railway.profile

import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import eu.ggnext.common.db.MongoManager
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

    private val activeProfiles = ConcurrentHashMap<UUID, RailwayProfile>()

    suspend fun createProfile(
        player: Player,
        name: String,
    ): RailwayProfile? {
        val currentProfileAmount =
            profileIndexCollection
                .find(Filters.eq("_id", player.uniqueId))
                .firstOrNull()
                ?.profileIds
                ?.size
                ?: 0

        if (currentProfileAmount >= maxRailwayAccounts) return null
        if (name.length >= maxNameLength) return null

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
        profileIndexCollection.updateOne(
            Filters.eq("_id", player.uniqueId),
            Updates.addToSet("profileIds", profileId),
            UpdateOptions().upsert(true),
        )
        return railwayProfile
    }

    suspend fun updateProfile(profile: RailwayProfile) = profileCollection.replaceOne(Filters.eq("_id", profile.id), profile)

    suspend fun getProfile(profileId: UUID): RailwayProfile? = profileCollection.find(Filters.eq("_id", profileId)).firstOrNull()

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
        if (getActiveProfile(player)?.id == profileId) {
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
        activeProfiles[player.uniqueId] = activeProfile
    }

    fun getActiveProfile(player: Player): RailwayProfile? = activeProfiles[player.uniqueId]

    fun deleteActiveProfile(player: Player) = activeProfiles.remove(player.uniqueId)

    suspend fun addDollars(
        profile: RailwayProfile,
        amount: Double,
    ) = profileCollection.updateOne(Filters.eq("_id", profile.id), Updates.inc("railwayDollars", amount))

    suspend fun removeDollars(
        profile: RailwayProfile,
        amount: Double,
    ): Boolean {
        if (profile.railwayDollars < amount) return false
        profileCollection.updateOne(Filters.eq("_id", profile.id), Updates.inc("railwayDollars", -amount))
        return true
    }

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
    ): Boolean {
        if (profile.level.skillPoints < amount) return false
        profileCollection.updateOne(Filters.eq("_id", profile.id), Updates.inc("level.skillPoints", -amount))
        return true
    }

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
    ) {
        profileCollection.updateOne(
            Filters.and(Filters.eq("_id", profile.id), Filters.eq("activeQuests.questId", questId)),
            Updates.set("activeQuests.$.currentValue", questProgress.currentValue),
        )
    }
}
