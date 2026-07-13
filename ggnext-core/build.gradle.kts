plugins {
    id("ggnext.paper-conventions")
}

dependencies {
    api(libs.bundles.ggnext.core)
    api(project(":contentsystem-sdk"))
    runtimeOnly(libs.scoreboard.library.implementation)
}