plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    implementation(project(":repository"))
    implementation(project(":ui-common"))
    implementation(project(":util"))

    implementation(compose.desktop.currentOs)

    implementation(libs.compose.material.icons.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.immutable.collections)
    implementation(libs.material.context.menu)
    implementation(libs.okhttp)

    testImplementation(libs.kotlinx.coroutines.test)

    testFixturesImplementation(project(":repository"))

    testFixturesImplementation(compose.runtime)
    testFixturesImplementation(compose.ui) // required for opt-ins
    testFixturesImplementation(compose.foundation) // required for opt-ins
    testFixturesImplementation(libs.assertk)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().matching { it.name == "test" }.configureEach {
            dependencies {
                implementation(testFixtures(project(":network")))
                implementation(testFixtures(project(":util")))
            }
        }

        withType<JvmTestSuite>().matching { it.name == "screenshotTest" }.configureEach {
            dependencies {
                implementation(testFixtures(project(":ui-common")))
            }
        }
    }
}
