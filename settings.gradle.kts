rootProject.name = "kotify"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            // https://github.com/willowtreeapps/assertk
            version("assertk", "0.25").let { version ->
                library("assertk", "com.willowtreeapps.assertk", "assertk-jvm").versionRef(version)
            }

            // https://github.com/jetbrains/compose-jb
            version("compose", "1.2.1").let { version ->
                plugin("compose", "org.jetbrains.compose").versionRef(version)

                library("compose-swing", "org.jetbrains.kotlinx", "kotlinx-coroutines-swing").versionRef(version)
                library("compose-test-junit4", "org.jetbrains.compose.ui", "ui-test-junit4").versionRef(version)
            }

            // https://github.com/Kotlin/kotlinx.coroutines
            version("coroutines", "1.6.4").let { version ->
                library("coroutines-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef(version)
                library("coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef(version)
            }

            // https://github.com/detekt/detekt
            version("detekt", "1.22.0-RC3").let { version ->
                plugin("detekt", "io.gitlab.arturbosch.detekt").versionRef(version)

                library("detekt-formatting", "io.gitlab.arturbosch.detekt", "detekt-formatting").versionRef(version)
            }

            // https://github.com/JetBrains/Exposed
            version("exposed", "0.41.1").let { version ->
                library("exposed-core", "org.jetbrains.exposed", "exposed-core").versionRef(version)
                library("exposed-dao", "org.jetbrains.exposed", "exposed-dao").versionRef(version)
                library("exposed-jdbc", "org.jetbrains.exposed", "exposed-jdbc").versionRef(version)
                library("exposed-java-time", "org.jetbrains.exposed", "exposed-java-time").versionRef(version)

                bundle("exposed", listOf("exposed-core", "exposed-dao", "exposed-jdbc", "exposed-java-time"))
            }

            // https://github.com/jacoco/jacoco
            version("jacoco", "0.8.8")

            // https://junit.org/junit4/
            version("junit4", "4.13.2").let { version ->
                library("junit4", "junit", "junit").versionRef(version)
            }

            // https://junit.org/junit5/
            version("junit5", "5.9.1").let { version ->
                library("junit5-api", "org.junit.jupiter", "junit-jupiter-api").versionRef(version)
                library("junit5-params", "org.junit.jupiter", "junit-jupiter-params").versionRef(version)
                library("junit5-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef(version)
                library("junit5-vintage-engine", "org.junit.vintage", "junit-vintage-engine").versionRef(version)

                bundle("junit5-api", listOf("junit5-api", "junit5-params"))
            }

            // https://kotlinlang.org/releases.html
            version("kotlin", "1.7.20")

            // https://github.com/Kotlin/kotlinx.serialization
            version("kotlinx-serialization", "1.4.1").let { version ->
                library("kotlinx-serialization", "org.jetbrains.kotlinx", "kotlinx-serialization-json")
                    .versionRef(version)
            }

            // https://ktor.io/changelog/
            version("ktor", "2.1.3").let { version ->
                library("ktor-netty", "io.ktor", "ktor-server-netty").versionRef(version)

                library("ktor-client", "io.ktor", "ktor-client-java").versionRef(version)
            }

            // https://github.com/mockk/mockk
            version("mockk", "1.13.2").let { version ->
                library("mockk", "io.mockk", "mockk").versionRef(version)
            }

            // https://square.github.io/okhttp
            version("okhttp", "4.10.0").let { version ->
                library("okhttp", "com.squareup.okhttp3", "okhttp").versionRef(version)
            }

            // https://github.com/Guardsquare/proguard
            version("proguard", "7.2.2").let { version ->
                library("proguard", "com.guardsquare", "proguard-gradle").versionRef(version)
            }

            // http://www.slf4j.org/
            version("slf4j", "1.7.32").let { version ->
                library("slf4j-nop", "org.slf4j", "slf4j-nop").versionRef(version)
            }

            // https://github.com/xerial/sqlite-jdbc
            version("sqlite-jdbc", "3.39.4.0").let { version ->
                library("sqlite-jdbc", "org.xerial", "sqlite-jdbc").versionRef(version)
            }
        }
    }
}
