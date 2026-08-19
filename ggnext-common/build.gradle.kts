plugins {
    id("ggnext.kotlin-conventions")
}

dependencies {
    api(project(":ggnext-protocol"))
    implementation(libs.kotlinx.serialization.json)
    compileOnly(libs.mongodb)
}