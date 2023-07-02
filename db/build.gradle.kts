plugins {
    alias(libs.plugins.detekt)
    id("jacoco")
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    implementation(project(":network")) // TODO required for translation from network models, move that to repos?

    testImplementation(testFixtures(project(":util")))

    api(libs.exposed.dao) // expose DAO classes in the API for access to Entity from dependencies

    implementation(libs.exposed.core)
    implementation(libs.exposed.javatime)
    implementation(libs.exposed.jdbc)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.sqlite.jdbc)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)
}
