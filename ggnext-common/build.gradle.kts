plugins {
    id("ggnext.kotlin-conventions")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    compileOnly(libs.mongodb)
}