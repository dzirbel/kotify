plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    api(project(":db"))
    api(project(":log"))
    api(project(":network"))
    implementation(project(":util"))

    testImplementation(testFixtures(project(":db")))
    testImplementation(testFixtures(project(":network")))
    testImplementation(testFixtures(project(":util")))

    testFixturesImplementation(project(":util"))
    testFixturesImplementation(testFixtures(project(":db")))
    testFixturesImplementation(testFixtures(project(":log")))
    testFixturesImplementation(testFixtures(project(":network")))

    implementation(compose.runtime)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.javatime)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)

    testFixturesImplementation(compose.runtime)
}
