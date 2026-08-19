package de.ggnext.sdk.feature

import de.ggnext.protocol.Channels
import de.ggnext.protocol.Routes
import de.ggnext.protocol.content.ContentEnsureDefaultRequest
import de.ggnext.protocol.content.ContentEnsureDefaultResponse
import de.ggnext.protocol.content.ContentEvent
import de.ggnext.protocol.content.ContentSnapshotRequest
import de.ggnext.protocol.content.ContentSnapshotResponse
import de.ggnext.sdk.EventBinding
import de.ggnext.sdk.FeatureId
import de.ggnext.sdk.FeatureModule
import de.ggnext.sdk.GGNext
import de.ggnext.sdk.SdkContext
import de.ggnext.sdk.rpc

interface ContentSdk {
    suspend fun snapshot(): List<String>

    suspend fun ensureDefault(documentJson: String): Boolean

    fun onChanged(handler: suspend (ContentEvent.Changed) -> Unit)

    fun onDeleted(handler: suspend (ContentEvent.Deleted) -> Unit)
}

internal class ContentSdkImpl(
    private val ctx: SdkContext,
) : ContentSdk {
    override suspend fun snapshot(): List<String> =
        ctx.rpc<ContentSnapshotRequest, ContentSnapshotResponse>(Routes.CONTENT_SNAPSHOT, ContentSnapshotRequest()).documents

    override suspend fun ensureDefault(documentJson: String): Boolean =
        ctx
            .rpc<ContentEnsureDefaultRequest, ContentEnsureDefaultResponse>(
                Routes.CONTENT_ENSURE_DEFAULT,
                ContentEnsureDefaultRequest(documentJson),
            ).created

    override fun onChanged(handler: suspend (ContentEvent.Changed) -> Unit) = ctx.events.on(ContentEvent.Changed::class.java, handler)

    override fun onDeleted(handler: suspend (ContentEvent.Deleted) -> Unit) = ctx.events.on(ContentEvent.Deleted::class.java, handler)
}

object Content : FeatureModule<ContentSdk> {
    override val id = FeatureId("content")

    override fun create(ctx: SdkContext): ContentSdk = ContentSdkImpl(ctx)

    override fun events(): List<EventBinding> =
        listOf(
            EventBinding(Channels.EVENT_CONTENT) { json, payload ->
                json.decodeFromJsonElement(ContentEvent.serializer(), payload)
            },
        )
}

val GGNext.content: ContentSdk get() = require(Content)
