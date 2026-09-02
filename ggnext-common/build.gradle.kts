plugins {
    id("ggnext.kotlin-conventions")
}

dependencies {
    implementation(libs.bundles.ggnext.common)
    compileOnly(libs.mongodb)
}