rootProject.name = "kotify"

include(
    "application",
    "db",
    "network",
    "repository",
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
