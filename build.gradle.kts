import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private object Versions {
    const val coroutines = "1.4.1" // https://github.com/Kotlin/kotlinx.coroutines
    const val detekt = "1.15.0-RC1" // https://github.com/detekt/detekt; also update plugin version
    const val fuel = "2.3.0" // https://github.com/kittinunf/fuel
    const val gson = "2.8.6" // https://github.com/google/gson
    const val junit = "5.7.0" // https://junit.org/junit5/
}

plugins {
    kotlin("jvm") version "1.4.20"
    id("io.gitlab.arturbosch.detekt") version "1.15.0-RC1"

    // https://plugins.gradle.org/plugin/name.remal.check-dependency-updates
    id("name.remal.check-dependency-updates") version "1.1.4"
}

version = "0.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("com.github.kittinunf.fuel", "fuel", Versions.fuel)
    implementation("com.github.kittinunf.fuel", "fuel-coroutines", Versions.fuel)
    implementation("com.github.kittinunf.fuel", "fuel-gson", Versions.fuel)
    implementation("com.google.code.gson", "gson", Versions.gson)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", Versions.coroutines)

    testImplementation("org.junit.jupiter", "junit-jupiter-api", Versions.junit)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", Versions.junit)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", Versions.junit)

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
}

detekt {
    input = files("src/main/kotlin", "src/test/kotlin")
    config = files("detekt-config.yml")
}

tasks.create<JavaExec>("run") {
    description = "Run Main.kt"
    group = "Build"

    main = "com.dominiczirbel.MainKt"
    classpath = sourceSets.main.get().runtimeClasspath
}
