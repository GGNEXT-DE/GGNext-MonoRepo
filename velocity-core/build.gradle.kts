plugins {
    id("ggnext.kotlin-conventions")
    kotlin("kapt") version "2.4.0"
    id("xyz.jpenilla.run-velocity") version "3.0.2"
    id("com.gradleup.shadow") version "9.4.1"
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    implementation(libs.bundles.ggnext.velocity)
    implementation(project(":contentsystem-sdk"))
}

tasks {
    runVelocity {
        velocityVersion("3.5.0-SNAPSHOT")
        downloadPlugins {
            modrinth("luckperms", "v5.5.53-velocity")
            modrinth("SignedVelocity", "1.4.1")
        }
    }
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        mergeServiceFiles {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}