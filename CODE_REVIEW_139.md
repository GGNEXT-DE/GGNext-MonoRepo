# Code Review: feature/139-player-profile-own-name

**Branch:** `feature/139-player-profile-own-name` vs `main`
**Commit:** feat: implemented base functions for custom profile name creation
**Date:** 2026-09-11

---

## 🎯 Zusammenfassung

Der Branch implementiert es, dass Spieler ihren Profilnamen selbst eingeben können, statt einen zufälligen Namen zu erhalten. Obwohl das Feature-Ziel gut ist, gibt es mehrere **kritische Probleme** und **Regressions**.

**Status:** ⚠️ **NICHT MERGE-READY** - Mehrere kritische Bugs müssen behoben werden

---

## ❌ Kritische Probleme

### 1. **Memory Leak: Cleanup Task entfernt**
**Datei:** `PlayerInputManager.kt`, `GGNextCore.kt`
**Severity:** 🔴 KRITISCH

```kotlin
// ENTFERNT:
fun startCleanupTask() {
    GGNextAPI.jobManager.registerJob(PlayerInputCleanupJob())
}
```

**Problem:**
- `playerInputManager.startCleanupTask()` wurde aus `GGNextCore.kt` entfernt
- Die `startCleanupTask()` Methode wurde komplett aus `PlayerInputManager` gelöscht
- Abgelaufene Input-Sessions werden **NIE** bereinigt → **Memory Leak**
- Nach 5+ Minuten sammeln sich alte Sessions an

**Auswirkung:** Speicherverschwendung, möglicherweise Server-Crash nach Tagen

**Fix erforderlich:**
```kotlin
// In GGNextCore.kt (line 87 - nach jobManager Init):
playerInputManager.startCleanupTask()

// In PlayerInputManager.kt - startCleanupTask() beibehalten!
```

---

### 2. **Fehlende Player Quit Handler**
**Datei:** `PlayerInputListener.kt`
**Severity:** 🔴 KRITISCH

```kotlin
// ENTFERNT:
@EventHandler
fun onPlayerQuit(event: PlayerQuitEvent) {
    plugin.launch {
        inputManager.cancelInput(event.player)
    }
}
```

**Problem:**
- Wenn ein Spieler disconnected, während er Input erwartet, wird die Session nicht bereinigt
- Dies führt zu orphaned Sessions mit Spielern, die offline sind
- Die Callback wird später versucht, auf einen Offline-Spieler zu wirken

**Auswirkung:** UI-Bugs, Error Logs, Memory Leak

**Fix erforderlich:**
- `onPlayerQuit` Handler wieder hinzufügen und `cancelInput` als public behalten

---

### 3. **Kritische Funktionen komplett entfernt**
**Datei:** `RailwayProfileManager.kt`
**Severity:** 🔴 KRITISCH

```kotlin
// ENTFERNT:
suspend fun addActiveQuest(profile, questId, type) { ... }
suspend fun getActiveQuests(profile) { ... }
suspend fun updateActiveQuest(profile, questId, progress) { ... }
suspend fun completeQuest(profile, questId) { ... }
```

**Problem:**
- Quest-Tracking ist offensichtlich kaputt
- Wenn dieser Code in anderen Dateien verwendet wird, gibt es Kompilierungsfehler
- Die komplette Quest-Management-Funktionalität wurde gelöscht

**Prüfung erforderlich:**
```bash
grep -r "addActiveQuest\|getActiveQuests\|updateActiveQuest" railway/
grep -r "addActiveQuest\|getActiveQuests\|updateActiveQuest" ggnext-core/
```

---

### 4. **Validation komplett entfernt**
**Datei:** `PlayerInputManager.kt`
**Severity:** 🟠 HOCH

```kotlin
// ENTFERNT:
if (message.isEmpty()) return
if (message.length > MAX_INPUT_LENGTH) { ... }
if (message.startsWith("/")) { 
    player.sendMessage("Commands are not allowed as input")
}
```

**Problem:**
- **Kommando-Injection möglich:** Spieler kann `/gamemode creative` eingeben → wird als Profilname akzeptiert
- **SQL-Injection ähnlich:** Sicherheitslücke bei Input-Validierung
- Leere Namen möglich
- Keine Längenbeschränkung (Spieler könnte Millionen Zeichen eingeben)

**Fix erforderlich:**
```kotlin
fun processInput(player: Player, message: String) {
    if (message.isEmpty()) return
    if (message.length > MAX_INPUT_LENGTH) {
        player.sendMessage(Component.text("Input too long!"))
        return
    }
    if (message.startsWith("/")) {
        player.sendMessage(Component.text("Commands not allowed!"))
        return
    }
    // ...
}
```

---

## 🟠 High-Priority Probleme

### 5. **InputValidation Bug Fix wurde angewendet**
**Datei:** `RailwayProfileManager.kt`, Line 51
**Severity:** 🟡 INFO (ist eigentlich Bugfix)

```kotlin
// VOR: if (name.length >= maxNameLength)
// NACH: if (name.length > maxNameLength)
```

**Status:** ✅ Das ist korrekt, aber sollte als separate Bugfix-PR eingereicht werden, nicht als Teil dieses Features

---

### 6. **Logging-Rückschritt**
**Datei:** `PlayerInputManager.kt`, `RailwayProfileManager.kt`
**Severity:** 🟡 MEDIUM

```kotlin
// VOR (Code Review):
import eu.ggnext.common.logging.log
log.warn("Error occurred")

// NACH:
import eu.ggnext.common.logging.LogControl
val logger = LogControl.logger(this::class.java)
logger.warn("Error occurred")
```

**Problem:**
- Rückschritt vom neuen Logging-System zum alten `LogControl`
- Inkonsistent mit den vorherigen Code Review Fixes
- Alle anderen Dateien nutzen `log.warn()` von `ggnext.common.logging`

**Fix erforderlich:**
```kotlin
import eu.ggnext.common.logging.log
import eu.ggnext.common.logging.warn

log.warn("...")  // nicht LogControl.logger()
```

---

### 7. **Translation Store entfernt, hardcoded Text hinzugefügt**
**Datei:** `PlayerInputManager.kt`, Line 53
**Severity:** 🟡 MEDIUM

```kotlin
// VOR:
val alreadyActiveKey by TranslationStore("translations.core.input.already_active")
player.sendMessage(alreadyActiveKey.get(player.language()))

// NACH:
player.sendMessage(Component.text("You already have an active input request!", NamedTextColor.RED))
```

**Problem:**
- Keine i18n Support (Internationalisierung) mehr
- Hardcoded englischer Text
- Inkonsistent mit anderen Nachrichten im System

**Fix erforderlich:**
```kotlin
private val alreadyActiveKey by TranslationStore("translations.core.input.already_active")

player.sendMessage(alreadyActiveKey.get(player.language()).color(NamedTextColor.RED))
```

---

### 8. **Große Listener und Manager gelöscht**
**Dateien:** Multiple gelöschte Dateien
**Severity:** 🟡 MEDIUM

**Gelöschte Dateien:**
- `railway/scoreboard/RailwayScoreBoardManager.kt` (228 Zeilen!)
- `railway/scoreboard/listener/RailwayScoreBoardListener.kt`
- `railway/scoreboard/listener/RailwayProfilePreloadListener.kt`
- `railway/quest/QuestListener.kt`
- `dc-bot/verify/VerifyCommand.kt`

**Problem:**
- Diese Dateien wurden komplett gelöscht, nicht refaktoriert
- Unklar ob die Funktionalität noch irgendwo existiert
- Nur ein Commit mit viel Bewegung → schwer zu verfolgen

**Frage für den Author:**
- Wurde die Scoreboard-Funktionalität in andere Dateien verschoben?
- Werden Quests nicht mehr unterstützt?
- Was ist mit dem Discord Verify System?

---

### 9. **Private Methoden machen cancelInput public unmöglich**
**Datei:** `PlayerInputManager.kt`
**Severity:** 🟡 MEDIUM

```kotlin
// WURDE PRIVATE:
private suspend fun cancelInput(
    player: Player,
    session: InputSession,
)
```

**Problem:**
- `cancelInput` ist jetzt `private` und nimmt `session` als Parameter
- `PlayerInputListener.onPlayerQuit()` konnte es aufrufen (ist jetzt entfernt)
- Wenn wir es wieder hinzufügen, können wir es nicht aufrufen!

**Lösung:**
```kotlin
suspend fun cancelInput(player: Player) {
    val session = activeInputs[player.uniqueId] ?: return
    // ...
}
```

---

## 🟢 Positive Aspekte

### ✅ Feature: Benutzerdefinierte Profilnamen
**Datei:** `ProfileGui.kt`

```kotlin
GGNextAPI.playerInputManager.requestInput(
    player = player,
    callback = { profileName ->
        val profile = profileManager.createProfile(player, profileName)
        // ...
    },
    previousGuiReopener = suspend { openProfileGui(player) }
)
```

**Gut:**
- Elegante Integration mit dem Input-System
- GUI wird wieder geöffnet nach Input
- Callback-Pattern ist sauber
- Bessere UX als zufälliger Name

---

### ✅ Build-Konfiguration behalten
**Datei:** `railway/build.gradle.kts`

- Kotlin excludes bleiben erhalten ✅
- Aber: `cleanWorldLock` Task wurde entfernt ❌

---

## 📋 Fragen für den Author

1. **Sind die gelöschten Dateien intentional entfernt?**
   - Scoreboard-System
   - Quest-System
   - Discord Verify
   
2. **Wird die Scoreboard-Funktionalität in einem späteren Commit hinzugefügt?**

3. **Sind die großen Änderungen Teil eines größeren Refactorings?**

---

## 🔧 Empfohlene Änderungen

### Priority 1 (BLOCKER): 
- [ ] `startCleanupTask()` in `PlayerInputManager` und `GGNextCore` zurück
- [ ] `onPlayerQuit` Handler in `PlayerInputListener` zurück
- [ ] Input Validation für Sicherheit zurück
- [ ] `cancelInput` public machen

### Priority 2 (HIGH):
- [ ] Logging zu `log.warn()` statt `LogControl.logger()` ändern
- [ ] Translation Keys statt hardcoded Text
- [ ] `cancelInput` nicht private machen

### Priority 3 (MEDIUM):
- [ ] Git History aufklären: Warum wurden 228 Zeilen Scoreboard gelöscht?
- [ ] Prüfung: Gibt es Kompilierungsfehler durch fehlende Methods?
- [ ] `cleanWorldLock` Task in Gradle zurück

### Priority 4 (NICE-TO-HAVE):
- [ ] Bessere Exception-Handling
- [ ] Logging für Debugging verbessern (viele neue logger.info() Aufrufe)

---

## 💬 Fazit

Das Feature selbst (Benutzer können Profilnamen eingeben) ist **gut**, aber die Implementierung hat **mehrere kritische Bugs** und **Regressions**:

❌ **Memory Leaks** (2)
❌ **Security Issues** (Input Validation)
❌ **Broken Functionality** (Quest System)
⚠️ **Code Quality Issues** (Logging, i18n)

**Recommendation:** 
- ❌ Nicht mergen in aktueller Form
- 🔧 Mit Author diskutieren über die gelöschten Features
- 📝 Separate PR für Bugfixes einreichen
- ✅ Nach Fixes nochmal reviewen

