import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private object Versions {
    const val detekt = "1.7.0"
    const val gson = "2.8.6"
    const val junit = "5.6.1"
    const val okhttp = "4.4.1"
}

plugins {
    kotlin("jvm") version "1.3.70"
    id("io.gitlab.arturbosch.detekt") version "1.7.0"
    id("name.remal.check-dependency-updates") version "1.0.178"
}

version = "0.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8", "1.3.70"))

    implementation("com.google.code.gson", "gson", Versions.gson)
    implementation("com.squareup.okhttp3", "okhttp", Versions.okhttp)

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
