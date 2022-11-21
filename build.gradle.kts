import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import proguard.gradle.ProGuardTask
import java.util.Properties

buildscript {
    dependencies {
        classpath(libs.proguard)
    }
}

plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    kotlin("plugin.serialization") version libs.versions.kotlin.get()

    alias(libs.plugins.detekt)
    alias(libs.plugins.compose)

    `java-test-fixtures`

    jacoco
}

val appProperties = file("src/main/resources/app.properties").inputStream().use { Properties().apply { load(it) } }

version = appProperties["version"] as String

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)

    implementation(libs.okhttp)
    implementation(libs.ktor.netty)
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.nop)

    implementation(libs.bundles.exposed)
    implementation(libs.sqlite.jdbc)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    // JUnit 4 is required to run Compose tests
    testCompileOnly(libs.junit4)
    testRuntimeOnly(libs.junit5.engine.vintage)
    testImplementation(libs.compose.junit4)

    testImplementation(libs.assertk)
    testImplementation(libs.ktor.client)
    testImplementation(libs.coroutines.swing) // Swing dispatcher for screenshot tests

    testFixturesImplementation(libs.assertk)
    testFixturesImplementation(libs.bundles.exposed)
    testFixturesImplementation(compose.desktop.currentOs)
    testFixturesImplementation(libs.kotlinx.serialization.json)
    testFixturesImplementation(libs.okhttp)
    testFixturesImplementation(libs.coroutines.core)

    detektPlugins(libs.detekt.formatting)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.allWarningsAsErrors = true
    kotlinOptions.jvmTarget = "16"

    // enable context receivers: https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"

    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlinx.coroutines.FlowPreview"
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.contracts.ExperimentalContracts"
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi" // allow use of GlobalScope
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    kotlinOptions.freeCompilerArgs += "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
    kotlinOptions.freeCompilerArgs += "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
    kotlinOptions.freeCompilerArgs += "-opt-in=androidx.compose.material.ExperimentalMaterialApi"
}

configurations.all {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
}

val testLocal = tasks.create<Test>("testLocal") {
    useJUnitPlatform {
        excludeTags("network")
    }
}

val testIntegration = tasks.create<Test>("testIntegration") {
    useJUnitPlatform {
        includeTags("network")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Test>().configureEach {
    systemProperty("junit.jupiter.extensions.autodetection.enabled", true)
    testLogging {
        events = setOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_ERROR, TestLogEvent.STANDARD_OUT)
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.create<JacocoReport>("jacocoTestReportLocal") {
    dependsOn(testLocal)
    executionData(testLocal)
    sourceSets(sourceSets.main.get())
}

tasks.create<JacocoReport>("jacocoTestReportIntegration") {
    dependsOn(testIntegration)
    executionData(testIntegration)
    sourceSets(sourceSets.main.get())
}

tasks.withType<JacocoReport> {
    reports {
        xml.required.set(true)
        csv.required.set(false)
    }
}

tasks.create<Task>("checkLocal") {
    dependsOn("detektWithTypeResolution")
    dependsOn("testLocal")
}

tasks.detektTestFixtures.configure {
    // enable type resolution for detekt on test fixtures
    jvmTarget = "16"
    classpath.from(files("src/testFixtures"))
}

// run with type resolution; see https://detekt.dev/docs/gettingstarted/type-resolution
tasks.create("detektWithTypeResolution") {
    dependsOn(tasks.detektMain)
    dependsOn(tasks.detektTest)
    dependsOn(tasks.detektTestFixtures)
}

detekt {
    source = files("src")
    config = files("detekt-config.yml")
}

val File.obfuscatedJarFile: File
    get() = File("${project.buildDir}/tmp/obfuscated/${nameWithoutExtension}.min.jar")

val proGuard = tasks.create<ProGuardTask>("proGuard") {
    dependsOn(tasks.jar.get())

    val allJars = tasks.jar.get().outputs.files + sourceSets.main.get().runtimeClasspath
        .filter { it.extension == "jar" }
        // workaround for https://github.com/JetBrains/compose-jb/issues/1971
        .filterNot { it.name.startsWith("skiko-awt-") && !it.name.startsWith("skiko-awt-runtime-") }

    for (file in allJars) {
        injars(file)
        outjars(file.obfuscatedJarFile)
    }

    libraryjars("${compose.desktop.application.javaHome}/jmods")

    configuration("proguard-rules.pro")
}

compose.desktop {
    application {
        // workaround for https://github.com/JetBrains/compose-jb/issues/188
        if (OperatingSystem.current().isLinux) {
            jvmArgs("-Dsun.java2d.uiScale=2.0")
        }

        mainClass = "com.dzirbel.kotify.MainKt"

        nativeDistributions {
            modules("jdk.crypto.ec") // required for SSL, see https://github.com/JetBrains/compose-jb/issues/429

            targetFormats(TargetFormat.Deb, TargetFormat.Exe)
            packageName = appProperties["name"] as String
            packageVersion = project.version.toString()
        }

        if (project.hasProperty("shrink")) {
            disableDefaultConfiguration()
            fromFiles(proGuard.outputs.files.asFileTree)
            mainJar.set(tasks.jar.map { RegularFile { it.archiveFile.get().asFile.obfuscatedJarFile } })
        }
    }
}

// override compose configuration of arguments so that they're only applied to :run and not when packaging the
// application
project.afterEvaluate {
    tasks.withType<JavaExec> {
        args = listOf(".kotify/cache", ".kotify/settings")
    }
}
