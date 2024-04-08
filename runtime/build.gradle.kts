plugins {
    alias(libs.plugins.detekt)
    id("jacoco")
    kotlin("jvm") version libs.versions.kotlin
}
