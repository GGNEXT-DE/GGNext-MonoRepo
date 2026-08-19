plugins {
    id("ggnext.paper-conventions")
}

dependencies {
    api(libs.bundles.ggnext.core)
    api(project(":ggnext-common"))
    api(project(":ggnext-sdk"))
    runtimeOnly(libs.scoreboard.library.implementation)
}