rootProject.name = "kotify"

include(
    "application",
    "code-coverage-report",
    "db",
    "log",
    "network",
    "repository",
    "runtime",
    "screenshot-test",
    "ui-common",
    "ui-kotify",
    "util",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }
}
