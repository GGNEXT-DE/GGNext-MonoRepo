plugins {
    id("ggnext.application-conventions")
}

dependencies {
    implementation(project(":ggnext-protocol"))
    implementation(project(":ggnext-transport"))
    implementation(project(":ggnext-common"))
    implementation(libs.bundles.ggnext.backend)
}

application {
    mainClass.set("de.ggnext.backend.MainKt")
}
