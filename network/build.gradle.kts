repositories {
    mavenCentral()
}

plugins {
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin
}

dependencies {
    testImplementation(testFixtures(project(":util")))

    testFixturesImplementation(testFixtures(project(":util")))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.netty)
    implementation(libs.okhttp)
    implementation(libs.slf4j.nop) // disables logging from ktor server

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client)

    testFixturesImplementation(libs.assertk)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlinx.serialization.json)
    testFixturesImplementation(libs.okhttp)
}
