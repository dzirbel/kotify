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

    implementation(compose.desktop.currentOs)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.immutable.collections)
    implementation(libs.okhttp)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)

    testFixturesImplementation(compose.desktop.currentOs)
    testFixturesImplementation(libs.assertk)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
}
