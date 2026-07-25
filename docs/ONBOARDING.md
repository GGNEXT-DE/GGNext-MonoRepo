# Onboarding Guide

Welcome to the GGNext Kotlin codebase! This guide will help you understand the core architectural concepts and patterns used throughout the project.


## Table of Contents
1. [ContentSystem](ContentSystemSDK.md)
2. [Development Workflow](#development-workflow)
---

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