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
- **Docker**
- **Lefthook**: Fast Git hooks manager (see installation steps below)


## Building the Project
To build the project, follow these steps:
1. **Clone the Repository**: [GGNext-MonoRepo](https://github.com/GGNEXT-DE/GGNext-MonoRepo) 
2. **Start Docker-compose**:
```bash
docker-compose up -d
```
3. Activate Replica Set
```bash
docker exec -it CONTAINER_NAME mongosh
```
Then:
```
rs.initiate({
  _id: "rs0",
  members: [
    {
      _id: 0,
      host: "localhost:27017"
    }
  ]
})
```
Finally:
```bash
exit
```
---

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