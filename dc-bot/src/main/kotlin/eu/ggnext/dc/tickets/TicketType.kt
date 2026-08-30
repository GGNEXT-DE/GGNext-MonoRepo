package eu.ggnext.dc.tickets

enum class TicketType(
    val id: String,
) {
    SUPPORT("support"),
    BUG_REPORT("bug-report"),
    USER_REPORT("user-report"),
    APPLICATION("application"),
}
