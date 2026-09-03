package eu.ggnext.velocityCore.features.punishment

import com.velocitypowered.api.proxy.Player
import eu.ggnext.contentsystem.value.store.TranslationStore
import eu.ggnext.velocityCore.utils.language
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object HistoryFormatter {
    private val dateFormatter =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())

    fun format(
        player: Player,
        targetName: String,
        filter: PunishmentHistoryFilter,
        punishments: List<PunishmentData>,
        site: Int,
    ): List<Component> {
        val header by TranslationStore("translations.velocity.punishment.history.header")

        val footer by TranslationStore("translations.velocity.punishment.history.footer")

        val maxSites: Int =
            punishments.size / 10 + (
                1
            )

        if (punishments.isEmpty()) {
            val msg1 by TranslationStore("translations.velocity.punishment.history.empty")
            return listOf(
                header.get(player.language(), listOf(targetName, filter.name.lowercase())),
                msg1.get(player.language()),
            )
        }

        return listOf(header.get(player.language(), listOf(targetName, filter.name.lowercase()))) +
            punishments
                .subList(
                    10 * site - 10,
                    if (punishments.size >
                        10 * site
                    ) {
                        10 * site
                    } else {
                        punishments.lastIndex + 1
                    },
                ).map { punishment ->
                    Component
                        .text("${punishment.type.name} ", typeColor(punishment.type))
                        .append(Component.text(statusText(punishment), statusColor(punishment)))
                        .append(Component.text(" | ${formatTime(punishment.issuedAt)}", NamedTextColor.GRAY))
                        .append(Component.text(" | ${punishment.reason}", NamedTextColor.WHITE))
                } +
            listOf(
                footer.get(
                    player.language(),
                    listOf(
                        site.toString(),
                        maxSites.toString(),
                        targetName,
                        filter.name.lowercase(),
                        (if (site != 1) site - 1 else site).toString(),
                        (if (maxSites == site) site else site + 1).toString(),
                    ),
                ),
            )
    }

    private fun statusText(punishment: PunishmentData): String {
        if ((punishment.revokedBy != null) ||
            (punishment.expiresAt <= System.currentTimeMillis() && punishment.expiresAt != -1L)
        ) {
            return "inactive"
        }
        if (punishment.expiresAt == -1L) return "active, permanent"

        return "active, expires ${formatTime(punishment.expiresAt)}"
    }

    private fun statusColor(punishment: PunishmentData): NamedTextColor =
        if (punishment.revokedBy == null &&
            (punishment.expiresAt == -1L || punishment.expiresAt > System.currentTimeMillis())
        ) {
            NamedTextColor.GREEN
        } else {
            NamedTextColor.DARK_GRAY
        }

    private fun typeColor(type: PunishmentType): NamedTextColor =
        when (type) {
            PunishmentType.BAN, PunishmentType.TEMP_BAN -> NamedTextColor.RED
            PunishmentType.MUTE, PunishmentType.TEMP_MUTE -> NamedTextColor.YELLOW
            PunishmentType.WARN -> NamedTextColor.GOLD
        }

    private fun formatTime(timestamp: Long): String = dateFormatter.format(Instant.ofEpochMilli(timestamp))
}
