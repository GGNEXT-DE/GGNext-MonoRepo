plugins {
    id("ggnext.paper-conventions")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    compileOnly(libs.fawe.core)
    compileOnly(libs.fawe.bukkit) { isTransitive = false }
}

tasks.runServer {
    downloadPlugins {
        modrinth("fastasyncworldedit", "2.15.2")
    }
}