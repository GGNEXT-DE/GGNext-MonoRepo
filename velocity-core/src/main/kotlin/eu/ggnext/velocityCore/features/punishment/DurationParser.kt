package eu.ggnext.velocityCore.features.punishment

object DurationParser {
    private val pattern = Regex("(\\d+)([smhdwy])")

    fun parse(input: String): Long {
        val match =
            pattern.matchEntire(input.lowercase())
                ?: throw IllegalArgumentException("Invalid duration format: $input")

        val value = match.groupValues[1].toLong()
        val unit = match.groupValues[2]

        return value *
            when (unit) {
                "s" -> 1000L
                "m" -> 60_000L
                "h" -> 3_600_000L
                "d" -> 86_400_000L
                "w" -> 604_800_000L
                "y" -> 31_536_000_000L
                else -> throw IllegalArgumentException("Unknown unit: $unit")
            }
    }
}
