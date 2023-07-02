repositories {
    mavenCentral()
}

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    implementation(project(":repository"))
    implementation(project(":util"))
    implementation(project(":ui-common"))

    testImplementation(testFixtures(project(":network")))
    testImplementation(testFixtures(project(":ui-common")))
    testImplementation(testFixtures(project(":util")))

    testFixturesImplementation(project(":db"))
    testFixturesImplementation(project(":repository"))

    implementation(compose.desktop.currentOs)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.immutable.collections)
    implementation(libs.okhttp)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    // JUnit 4 is required to run Compose tests
    testCompileOnly(libs.junit4)
    testRuntimeOnly(libs.junit5.engine.vintage)
    testImplementation(libs.compose.junit4)

    testImplementation(libs.assertk)

    testFixturesImplementation(compose.desktop.currentOs)
    testFixturesImplementation(libs.assertk)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
}
