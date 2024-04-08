plugins {
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    implementation(project(":util"))

    api(libs.exposed.dao) // expose DAO classes in the API for access to Entity from dependencies

    implementation(libs.exposed.core)
    implementation(libs.exposed.javatime)
    implementation(libs.exposed.jdbc)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.sqlite.jdbc)

    testFixturesImplementation(libs.junit5.api)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().matching { it.name == "test" }.configureEach {
            dependencies {
                implementation(testFixtures(project(":util")))

                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
