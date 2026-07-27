import com.diffplug.spotless.LineEnding

plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
    kotlin("plugin.serialization")
    id("io.sentry.jvm.gradle")
}

kotlin {
    jvmToolchain(25)
}

spotless {
    kotlin {
        targetExclude("build/generated/**/*")
        targetExclude("build/generated-src/**/*")
        toggleOffOn()
        ktlint("1.8.0")
        lineEndings = LineEnding.GIT_ATTRIBUTES_FAST_ALLSAME
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
}