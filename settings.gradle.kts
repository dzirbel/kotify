rootProject.name = "kotify"

include(
    "db",
    "network",
    "repository",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
