package de.ggnext.railway.quest

import java.util.UUID

data class QuestProgress(
    val questId: String,
    val profileId: UUID,
    var currentValue: Int = 0,
    var completed: Boolean = false,
)