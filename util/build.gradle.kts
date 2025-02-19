plugins {
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.immutable.collections)

    testImplementation(libs.kotlinx.coroutines.test)

    testFixturesImplementation(libs.assertk)
    testFixturesImplementation(libs.junit5.api)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(libs.mockk)
    testFixturesImplementation(libs.slf4j.nop) // suppress warnings from mockk: https://github.com/mockk/mockk/issues/243
}
