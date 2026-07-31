plugins {
    id("ggnext.paper-conventions")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    compileOnly(libs.fawe.core)
    compileOnly(libs.fawe.bukkit) { isTransitive = false }

    implementation(project(":ggnext-common"))
}

tasks.runServer {
    downloadPlugins {
        modrinth("fastasyncworldedit", "2.15.2")
    }
}