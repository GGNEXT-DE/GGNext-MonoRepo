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
