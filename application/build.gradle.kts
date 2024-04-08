import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin
}

val appProperties = file("src/main/resources/app.properties").inputStream().use { Properties().apply { load(it) } }

version = appProperties["version"] as String

dependencies {
    implementation(project(":log"))
    implementation(project(":repository"))
    implementation(project(":runtime"))
    implementation(project(":ui-common"))
    implementation(project(":ui-kotify"))
    implementation(project(":util"))

    implementation(compose.desktop.currentOs)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.immutable.collections)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.material.context.menu)
    implementation(libs.okhttp)

    testFixturesImplementation(project(":repository"))
    testFixturesImplementation(project(":ui-common"))
    testFixturesImplementation(project(":ui-kotify"))
    testFixturesImplementation(project(":util"))
    testFixturesImplementation(testFixtures(project(":ui-common")))
    testFixturesImplementation(testFixtures(project(":ui-kotify")))
    testFixturesImplementation(testFixtures(project(":repository")))

    testFixturesApi(compose.desktop.currentOs)
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
                implementation(testFixtures(project(":db")))
                implementation(testFixtures(project(":network")))
                implementation(testFixtures(project(":repository")))
                implementation(testFixtures(project(":ui-common")))
                implementation(testFixtures(project(":ui-kotify")))
                implementation(testFixtures(project(":util")))
            }
        }
    }
}

compose.desktop {
    application {
        // workaround for https://github.com/JetBrains/compose-jb/issues/188
        if (OperatingSystem.current().isLinux) {
            jvmArgs("-Dsun.java2d.uiScale=2.0")
        }

        mainClass = "com.dzirbel.kotify.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(rootProject.file("proguard-rules.pro"))
        }

        nativeDistributions {
            modules("java.sql")
            modules("jdk.crypto.ec") // required for SSL, see https://github.com/JetBrains/compose-jb/issues/429

            targetFormats(
                // Linux
                // TODO AppImage breaks macOS builds: https://github.com/JetBrains/compose-multiplatform/issues/3814
                //  (and does not appear to be build a single-file AppImage)
                TargetFormat.Deb,
                TargetFormat.Rpm,

                // Windows
                TargetFormat.Exe,
                TargetFormat.Msi,

                // macOS
                // TODO adding both DMG and PKG formats results in an error:
                //  https://github.com/JetBrains/compose-multiplatform/issues/2233
                TargetFormat.Dmg,
            )

            packageName = appProperties["name"] as String
            packageVersion = project.version.toString()

            linux {
                menuGroup = "Audio"
                appCategory = "Audio"
                iconFile = project.file("src/main/resources/logo.png")
            }

            macOS {
                packageName = "com.dzirbel.kotify"
                appCategory = "public.app-category.music"
                iconFile = project.file("src/main/resources/logo.icns")
                packageVersion = appProperties["macVersion"] as String
                packageBuildVersion = appProperties["macVersion"] as String
            }

            windows {
                iconFile = project.file("src/main/resources/logo.ico")
            }
        }
    }
}

project.afterEvaluate {
    // TODO clean up once https://github.com/JetBrains/compose-multiplatform/issues/3700 is resolved

    // override compose configuration of arguments so that they're only applied to :run and not when packaging the
    // application
    tasks.withType<JavaExec> {
        addArgs("--cache-dir", "../.kotify/cache")
        addArgs("--settings-dir", "../.kotify/settings")
        addArgs("--log-dir", "../.kotify/logs")
    }

    // configure debug run task
    tasks.named<JavaExec>("run").configure {
        addArgs("--debug")
    }
}

fun JavaExec.addArgs(vararg args: String) {
    this.args = this.args.orEmpty().plus(args)
}
