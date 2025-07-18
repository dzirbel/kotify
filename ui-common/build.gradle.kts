plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    implementation(project(":util"))
    implementation(project(":runtime"))

    implementation(compose.desktop.currentOs)

    implementation(libs.compose.material.icons.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.immutable.collections)
    implementation(libs.material.context.menu)

    testFixturesImplementation(project(":screenshot-test"))
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().matching { it.name == "test" }.configureEach {
            dependencies {
                implementation(testFixtures(project(":util")))
            }
        }

        withType<JvmTestSuite>().matching { it.name == "screenshotTest" }.configureEach {
            dependencies {
                implementation(libs.compose.material.icons.core)
                implementation(libs.kotlinx.immutable.collections)
            }
        }
    }
}
