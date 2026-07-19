plugins {
    id("ggnext.velocity-conventions")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")

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