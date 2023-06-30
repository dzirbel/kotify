repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin

    alias(libs.plugins.detekt)

    `java-test-fixtures`

    jacoco
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.ktor.netty)
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.nop)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.assertk)
    testImplementation(libs.ktor.client)

    testFixturesImplementation(libs.okhttp)
    testFixturesImplementation(libs.coroutines.core)
    testFixturesImplementation(libs.kotlinx.serialization.json)
}
