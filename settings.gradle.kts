pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.noxcrew.com/public")
        maven("https://mvn.wesjd.net/")
    }
}

rootProject.name = "ggnext-monorepo"
include( "ggnext-core", "contentsystem-sdk", "velocity-core", "builder-plugin", "railway")