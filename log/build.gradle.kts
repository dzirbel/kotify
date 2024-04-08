plugins {
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    implementation(project(":util"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.immutable.collections)

    testFixturesImplementation(libs.kotlinx.coroutines.core)
}
