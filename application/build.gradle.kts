import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.detekt)
    id("jacoco")
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin
}

val appProperties = file("src/main/resources/app.properties").inputStream().use { Properties().apply { load(it) } }

version = appProperties["version"] as String

dependencies {
    implementation(project(":repository"))
    implementation(project(":ui-common"))
    implementation(project(":ui-kotify"))
    implementation(project(":util"))

    testImplementation(testFixtures(project(":db")))
    testImplementation(testFixtures(project(":network")))
    testImplementation(testFixtures(project(":repository")))
    testImplementation(testFixtures(project(":ui-common")))
    testImplementation(testFixtures(project(":ui-kotify")))
    testImplementation(testFixtures(project(":util")))

    implementation(compose.desktop.currentOs)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.immutable.collections)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    testImplementation(libs.assertk)
}

compose.desktop {
    application {
        // workaround for https://github.com/JetBrains/compose-jb/issues/188
        if (OperatingSystem.current().isLinux) {
            jvmArgs("-Dsun.java2d.uiScale=2.0")
        }

        mainClass = "com.dzirbel.kotify.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            modules("java.sql")
            modules("jdk.crypto.ec") // required for SSL, see https://github.com/JetBrains/compose-jb/issues/429

            targetFormats(TargetFormat.Deb, TargetFormat.Exe)
            packageName = appProperties["name"] as String
            packageVersion = project.version.toString()
        }
    }
}

// override compose configuration of arguments so that they're only applied to :run and not when packaging the
// application
project.afterEvaluate {
    tasks.withType<JavaExec> {
        args = listOf("../.kotify/cache", "../.kotify/settings")
    }
}
