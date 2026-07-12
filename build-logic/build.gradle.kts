plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.noxcrew.com/public")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.2")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.5")
    implementation("xyz.jpenilla.run-paper:xyz.jpenilla.run-paper.gradle.plugin:3.0.2")
    implementation("io.papermc.paperweight.userdev:io.papermc.paperweight.userdev.gradle.plugin:2.0.0-beta.21")
    implementation("org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:2.4.0")
    implementation("org.jetbrains.kotlin.kapt:org.jetbrains.kotlin.kapt.gradle.plugin:2.4.0")
    implementation("xyz.jpenilla.run-velocity:xyz.jpenilla.run-velocity.gradle.plugin:3.0.2")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.4.1")
}