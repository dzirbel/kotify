plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
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

    implementation(compose.runtime)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.javatime)
    implementation(libs.kotlinx.coroutines.core)

    testFixturesImplementation(project(":util"))
    testFixturesImplementation(testFixtures(project(":db")))
    testFixturesImplementation(testFixtures(project(":log")))
    testFixturesImplementation(testFixtures(project(":network")))

    testFixturesImplementation(compose.runtime)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().matching { it.name == "test" }.configureEach {
            dependencies {
                implementation(testFixtures(project(":db")))
                implementation(testFixtures(project(":network")))
                implementation(testFixtures(project(":util")))

                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockk)
            }
        }
    }
}
