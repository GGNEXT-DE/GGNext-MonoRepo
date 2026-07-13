# Onboarding Guide

Welcome to the GGNext Kotlin codebase! This guide will help you understand the core architectural concepts and patterns used throughout the project.


## Table of Contents
1. [ContentSystem](#contentsystem)
2. [Development Workflow](#development-workflow)
---

## ContentSystem
The ContentSystem is one of the most important concepts in the codebase. It is really important to understand it to work with our projects.

### Stores
The ContentSystem consists of so called `Stores`. Each store holds Key-Values for **one Type**, e.g. `TranslationStore` or `NumberStore`.

**Example**
```kotlin
val notFoundMsg by TranslationStore("translations.player_not_found")
player.sendMessage(notFoundMsg.get(player.language()))
```

### What not to do
These `Stores` need to be accessed via delegates (`by`) and **NOT** just as a variable, because then they won't be reloaded if their value changed.

```kotlin
// WRONG: not accessed via delegates
val notFoundMsg = TranslationStore("translations.player_not_found")
player.sendMessage(notFoundMsg.get(player.language())) 
```


## Development Workflow
1. **Create a new feature branch**:
```bash
   git checkout -b feature/my-new-feature
```

2. **Make your changes**:
    - Follow existing code style
    - Use Kotlin idioms (extension functions, data classes, etc.)
   
3. **Format code** (Spotless):
```bash
   ./gradlew spotlessApply
```

4. **Build and test**:
```bash
   ./gradlew build
```

5. **Commit and push**:
```bash
   git add .
   git commit -m "Add new feature"
   git push origin feature/my-new-feature
```