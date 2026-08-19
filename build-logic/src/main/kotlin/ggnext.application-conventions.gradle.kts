plugins {
    id("ggnext.kotlin-conventions")
    application
    id("com.gradleup.shadow")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }
}
