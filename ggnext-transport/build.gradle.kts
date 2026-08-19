plugins {
    id("ggnext.kotlin-conventions")
}

dependencies {
    api(project(":ggnext-protocol"))
    api(libs.bundles.ggnext.transport)
}
