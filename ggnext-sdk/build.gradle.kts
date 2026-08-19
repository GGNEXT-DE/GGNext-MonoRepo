plugins {
    id("ggnext.kotlin-conventions")
}

dependencies {
    api(project(":ggnext-protocol"))
    api(project(":ggnext-transport"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.bson)
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")
}
