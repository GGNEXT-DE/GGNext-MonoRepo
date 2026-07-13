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
