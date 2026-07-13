plugins {
    id("ggnext.paper-conventions")
}

dependencies {
    compileOnly(project(":ggnext-core"))

    compileOnly(libs.fawe.core)
    compileOnly(libs.fawe.bukkit) { isTransitive = false }
}

tasks {
    runServer {
        downloadPlugins {
            modrinth("fastasyncworldedit", "2.15.2")
            modrinth("luckperms", "v5.5.53-bukkit")
        }
    }

    shadowJar {
        dependencies {
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core.*"))
        }
    }
}