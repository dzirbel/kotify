
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private object Versions {
    const val coroutines = "1.4.3" // https://github.com/Kotlin/kotlinx.coroutines
    const val detekt = "1.16.0" // https://github.com/detekt/detekt; also update plugin version
    const val jacoco = "0.8.6" // https://github.com/jacoco/jacoco
    const val junit = "5.7.1" // https://junit.org/junit5/
    const val kotlinxSerialization = "1.0.1" // https://github.com/Kotlin/kotlinx.serialization
    const val okhttp = "4.9.1" // https://square.github.io/okhttp/
    const val truth = "1.1.2" // https://truth.dev/
}

plugins {
    // https://kotlinlang.org/releases.html
    kotlin("jvm") version "1.4.31"

    // https://github.com/Kotlin/kotlinx.serialization
    kotlin("plugin.serialization") version "1.4.31"

    // https://github.com/detekt/detekt; also update dependency version
    id("io.gitlab.arturbosch.detekt") version "1.16.0"

    // https://plugins.gradle.org/plugin/name.remal.check-dependency-updates
    id("name.remal.check-dependency-updates") version "1.3.0"

    // https://github.com/jetbrains/compose-jb
    id("org.jetbrains.compose") version "0.3.2"

    `java-test-fixtures`

    jacoco
}

version = "0.1"

repositories {
    mavenCentral()
    jcenter()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)

    implementation("com.squareup.okhttp3", "okhttp", Versions.okhttp)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", Versions.coroutines)
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", Versions.kotlinxSerialization)

    testImplementation("org.junit.jupiter", "junit-jupiter-api", Versions.junit)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", Versions.junit)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", Versions.junit)
    testImplementation("com.google.truth", "truth", Versions.truth)

    testFixturesImplementation(compose.desktop.currentOs)
    testFixturesImplementation("com.squareup.okhttp3", "okhttp", Versions.okhttp)
    testFixturesImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", Versions.coroutines)
    testFixturesImplementation("com.google.truth", "truth", Versions.truth)

    detektPlugins("io.gitlab.arturbosch.detekt", "detekt-formatting", Versions.detekt)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.allWarningsAsErrors = true
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.FlowPreview"
}

configurations.all {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
}

tasks.create<Test>("testLocal") {
    useJUnitPlatform {
        excludeTags("network")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Test>().configureEach {
    finalizedBy(tasks.jacocoTestReport)
    testLogging {
        events = setOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_ERROR, TestLogEvent.STANDARD_OUT)
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

jacoco {
    toolVersion = Versions.jacoco
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
    }
}

tasks.create<Task>("checkLocal") {
    dependsOn("detekt")
    dependsOn("testLocal")
}

detekt {
    input = files("src")
    config = files("detekt-config.yml")
}

compose.desktop {
    application {
        // workaround for https://github.com/JetBrains/compose-jb/issues/188
        if (OperatingSystem.current().isLinux) {
            jvmArgs("-Dsun.java2d.uiScale=2.0")
        }

        mainClass = "com.dominiczirbel.MainKt"
    }
}
