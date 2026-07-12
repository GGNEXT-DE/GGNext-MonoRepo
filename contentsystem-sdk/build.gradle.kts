plugins {
    id("ggnext.kotlin-conventions")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")

    compileOnly(libs.mongodb)
}