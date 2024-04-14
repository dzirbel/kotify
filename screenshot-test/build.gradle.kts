plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.detekt)
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    api(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing) // Swing dispatcher for screenshot tests
}
