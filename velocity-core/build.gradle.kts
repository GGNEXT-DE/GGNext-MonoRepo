plugins {
    id("ggnext.velocity-conventions")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.1.1")
    kapt("com.velocitypowered:velocity-api:4.1.1")

    implementation(libs.bundles.ggnext.velocity)
    implementation(project(":contentsystem-sdk"))
    implementation(project(":ggnext-common"))
    compileOnly(libs.nuvotifier.api)
    compileOnly(libs.nuvotifier.velocity)
}

tasks {
    runVelocity {
        velocityVersion("3.5.0-SNAPSHOT")
        downloadPlugins {
            modrinth("luckperms", "v5.5.53-velocity")
            modrinth("SignedVelocity", "1.4.1")
            github("nuvotifier", "NuVotifier", "v2.7.3", "nuvotifier.jar")
        }
    }
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }
}
