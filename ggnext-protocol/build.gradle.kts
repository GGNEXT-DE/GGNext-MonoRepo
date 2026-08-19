plugins {
    id("ggnext.kotlin-conventions")
}

dependencies {
    api(libs.kotlinx.serialization.json)
    compileOnly(libs.mongodb)
}
