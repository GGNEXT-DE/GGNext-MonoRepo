plugins {
    id("ggnext.paper-conventions")
}

dependencies {
    api(libs.bundles.ggnext.core)
    api(project(":contentsystem-sdk"))
    api(project(":ggnext-common"))
    runtimeOnly(libs.scoreboard.library.implementation)
}