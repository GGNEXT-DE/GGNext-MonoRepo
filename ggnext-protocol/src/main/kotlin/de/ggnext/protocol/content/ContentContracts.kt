package de.ggnext.protocol.content

import kotlinx.serialization.Serializable

@Serializable
data class ContentSnapshotRequest(
    val unit: Boolean = true,
)

@Serializable
data class ContentSnapshotResponse(
    val documents: List<String>,
)

@Serializable
data class ContentEnsureDefaultRequest(
    val documentJson: String,
)

@Serializable
data class ContentEnsureDefaultResponse(
    val created: Boolean,
)

@Serializable
sealed interface ContentEvent {
    @Serializable
    data class Changed(
        val documentJson: String,
    ) : ContentEvent

    @Serializable
    data class Deleted(
        val key: String,
    ) : ContentEvent
}
