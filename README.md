# GGNext-MonoRepo

GGNext-MonoRepo is the core structure for all Kotlin projects from GGNext.

## Modules
- **Build-Logic**: Central management for paper, velocity and Kotlin versions.
- **Builder-Plugin**: Plugin to help builders define Zones.
- **ContentSystem-SDK**: Key-Value Store for Translations and Numbers.
- **GGNext-Core**: Central paper core, with features like db-management, economy system and utils
- **Railway**: Plugin for our gamemode Railways
- **Velocity-Core**: Everything proxy, for example punishment maintenance etc.


## Requirements
- **Java**: Java 25
- **Lefthook**: Fast Git hooks manager (see installation steps below)

## Installing Lefthook

We use [Lefthook](https://github.com/evilmartians/lefthook) to manage Git hooks (such as linters and code formatting) across our Monorepo. Please install the CLI globally on your machine once:

### Windows

Via **Winget** (pre-installed on Windows 10/11) in PowerShell:

```powershell
winget install evilmartians.lefthook

```

*(Alternatively via Scoop: `scoop install lefthook`)*

### Arch Linux

Available in the AUR:

```bash
yay -S lefthook

```

### Activating Hooks

Once the CLI is installed, activate the hooks for this repository by running the following command in the project root:

```bash
lefthook install

```