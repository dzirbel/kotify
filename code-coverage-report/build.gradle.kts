plugins {
    id("jacoco-report-aggregation")

    // add to create check/test tasks by default and avoid strange dependency resolution errors
    kotlin("jvm") version libs.versions.kotlin
}

val integrationTest = tasks.create("integrationTest")

@Suppress("UnstableApiUsage")
reporting {
    reports {
        create<JacocoCoverageReport>("jacocoMergedUnitTestReport") {
            testType = TestSuiteType.UNIT_TEST
            tasks.check.configure { finalizedBy(reportTask) }
        }

        create<JacocoCoverageReport>("jacocoMergedScreenshotTestReport") {
            testType = "screenshot-test"
            tasks.check.configure { finalizedBy(reportTask) }
        }

        create<JacocoCoverageReport>("jacocoMergedIntegrationTestReport") {
            testType = TestSuiteType.INTEGRATION_TEST
            integrationTest.finalizedBy(reportTask)
        }
    }
}

dependencies {
    jacocoAggregation(project(":application")) // adds other modules transitively
}
