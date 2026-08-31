plugins {
    id("ggnext.kotlin-conventions")
    application
}

dependencies {
    implementation(libs.bundles.dc.bot)
    implementation(project(":contentsystem-sdk"))
    implementation(project(":ggnext-common"))
}

application {
    mainClass.set("eu.ggnext.dc.MainKt")
}