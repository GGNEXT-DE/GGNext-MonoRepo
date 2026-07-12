plugins {
    id("ggnext.kotlin-conventions")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
    id("io.papermc.paperweight.userdev")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    runServer {
        minecraftVersion("26.1.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }
    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}