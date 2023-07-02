plugins {
    alias(libs.plugins.compose) // TODO temporary for use of State in RatingRepository
    alias(libs.plugins.detekt)
    id("jacoco")
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    api(project(":db"))
    api(project(":network"))
    implementation(project(":util"))

    testImplementation(testFixtures(project(":util")))

    implementation(compose.desktop.currentOs)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.javatime)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)
}
