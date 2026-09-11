# ContentSystem
The ContentSystem is one of the most important concepts in the codebase. It is really important to understand it to work with our projects.

## Stores
The ContentSystem consists of so called `Stores`. Each store holds Key-Values for **one Type**, e.g. `TranslationStore` or `NumberStore`.

**Example**
```kotlin
val notFoundMsg by TranslationStore("translations.player_not_found")
player.sendMessage(notFoundMsg.get(player.language()))
```

## What not to do
These `Stores` need to be accessed via delegates (`by`) and **NOT** just as a variable, because then they won't be reloaded if their value changed.

```kotlin
// WRONG: not accessed via delegates
val notFoundMsg = TranslationStore("translations.player_not_found")
player.sendMessage(notFoundMsg.get(player.language())) 
```


## Special Cases

### QuestStore

The `QuestStore` is a special case compared to standard stores. Since quests are usually organized in groups or categories, this store returns a `List<Quest>` instead of a single value when accessed via the delegate. It automatically retrieves all keys that start with the specified prefix.

**Example**
```kotlin
// Loads all quests whose keys start with "quests.daily."
val dailyQuests by QuestStore("quests.daily.")

dailyQuests.forEach { quest ->
    player.sendMessage("Available quest: \${quest.name}")
}
```

## Key Naming

Keys follow a fixed schema:

```
{numbers|translations|strings|quests|skill_path}.{railway|velocity|core|discord}.{system}.{feature}[.{subfeature}].{name}
```

- Platform segment (`railway` or `velocity` or `core`) is always present.
- All segments are lowercase.
- Multi-word segments use snake_case (`already_banned`, not `alreadyBanned`).
- `system` is the module/manager the key belongs to (`auction`, `friendsystem`, `punishment`, ...), written as one word without underscores.

**Examples**

```
numbers.railway.auction.default_price
translations.railway.auction.gui.title
translations.velocity.punishment.ban.already_banned
translations.velocity.verification.active_process
```

Existing keys missing the platform segment or using camelCase are being migrated to this format; when touching a store, fix its key to match.
```