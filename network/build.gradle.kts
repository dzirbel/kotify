plugins {
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin
}

dependencies {
    implementation(project(":log"))
    implementation(project(":runtime"))
    implementation(project(":util"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.netty)
    implementation(libs.okhttp)
    implementation(libs.slf4j.nop) // disables logging from ktor server

    testFixturesImplementation(testFixtures(project(":util")))

    testFixturesImplementation(libs.assertk)
    testFixturesImplementation(libs.junit5.api)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlinx.serialization.json)
    testFixturesImplementation(libs.okhttp)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().matching { it.name == "test" }.configureEach {
            dependencies {
                implementation(libs.ktor.client)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project(":util"))
                implementation(testFixtures(project(":util")))

                implementation(libs.okhttp)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
