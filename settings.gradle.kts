rootProject.name = "kotify"

include(
    "db",
    "network",
    "repository",
    "util",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
