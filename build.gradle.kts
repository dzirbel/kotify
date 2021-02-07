import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private object Versions {
    const val bijectiveReflection = "2.0.0" // https://github.com/dzirbel/gson-bijectivereflection
    const val coroutines = "1.4.2" // https://github.com/Kotlin/kotlinx.coroutines
    const val detekt = "1.15.0" // https://github.com/detekt/detekt; also update plugin version
    const val gson = "2.8.6" // https://github.com/google/gson
    const val junit = "5.7.1" // https://junit.org/junit5/
    const val okhttp = "4.9.1" // https://square.github.io/okhttp/
    const val truth = "1.1.2" // https://truth.dev/
}

plugins {
    // https://kotlinlang.org/releases.html
    kotlin("jvm") version "1.4.21"

    // https://github.com/detekt/detekt; also update dependency version
    id("io.gitlab.arturbosch.detekt") version "1.15.0"

    // https://plugins.gradle.org/plugin/name.remal.check-dependency-updates
    id("name.remal.check-dependency-updates") version "1.2.2"

    // https://github.com/jetbrains/compose-jb
    id("org.jetbrains.compose") version "0.2.0-build132"
}

version = "0.1"

repositories {
    mavenCentral()
    jcenter()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)

    implementation("com.google.code.gson", "gson", Versions.gson)
    implementation("com.squareup.okhttp3", "okhttp", Versions.okhttp)
    implementation("io.github.dzirbel", "gson-bijectivereflection", Versions.bijectiveReflection)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", Versions.coroutines)

    testImplementation("org.junit.jupiter", "junit-jupiter-api", Versions.junit)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", Versions.junit)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", Versions.junit)
    testImplementation("com.google.truth", "truth", Versions.truth)

    detektPlugins("io.gitlab.arturbosch.detekt", "detekt-formatting", Versions.detekt)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.allWarningsAsErrors = true
    kotlinOptions.jvmTarget = "1.8"
}

configurations.all {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_ERROR, TestLogEvent.STANDARD_OUT)
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

detekt {
    input = files("src/main/kotlin", "src/test/kotlin")
    config = files("detekt-config.yml")
}

compose.desktop {
    application {
        mainClass = "com.dominiczirbel.MainKt"
    }
}
