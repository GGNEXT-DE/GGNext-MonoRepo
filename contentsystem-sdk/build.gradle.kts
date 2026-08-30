plugins {
    id("ggnext.kotlin-conventions")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.119-stable")

    compileOnly(libs.mongodb)
}