package eu.ggnext.railway.zone.instance

class ZoneInstanceManager {
    // true = free, false = occupied
    private val slots = mutableMapOf<Int, Boolean>()

    @Synchronized
    fun allocateSlot(): Int {
        val freeSlot =
            slots.entries
                .filter { it.value }
                .minByOrNull { it.key }
                ?.key
        if (freeSlot != null) {
            slots[freeSlot] = false
            return freeSlot
        }
        val newSlot = if (slots.isEmpty()) 0 else slots.keys.maxOrNull()?.plus(1) ?: 0
        slots[newSlot] = false
        return newSlot
    }

    @Synchronized
    fun freeSlot(slot: Int) {
        slots[slot] = true
    }
}
