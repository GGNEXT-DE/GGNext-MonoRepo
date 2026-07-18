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
            modrinth("fastasyncworldedit", "2.15.3")
            modrinth("luckperms", "v5.5.53-bukkit")
        }
        dependsOn("copyCore")
    }

    shadowJar {
        dependencies {
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core.*"))
        }
    }
    register<Copy>("copyCore") {
        description = "Copy Core build"
        dependsOn(":ggnext-core:shadowJar")

        from(project(":ggnext-core").tasks.named("shadowJar"))
        into(layout.projectDirectory.dir("run/plugins"))
    }
}