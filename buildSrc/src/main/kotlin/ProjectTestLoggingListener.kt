import org.gradle.api.Project
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.internal.logging.text.StyledTextOutput.Style
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.kotlin.dsl.support.serviceOf

class ProjectTestLoggingListener(project: Project) : TestListener {
    private val output = project.serviceOf<StyledTextOutputFactory>().create("colorful-test-output")

    init {
        System.setProperty("org.gradle.color.failure", "RED")
        System.setProperty("org.gradle.color.progressstatus", "YELLOW")
        System.setProperty("org.gradle.color.success", "GREEN")
        output.style(Style.Normal)
    }

    override fun beforeSuite(suite: TestDescriptor) {
        if (suite.name.startsWith("Test Run") || suite.name.startsWith("Gradle Worker")) {
            return
        }
        output.style(Style.Normal).println("${System.lineSeparator()}${suite.name}")
    }

    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        if (suite.parent == null) {
            return
        }
        output.style(Style.Normal)
            .println(
                "Results: ${result.resultType} " +
                    "(${result.testCount} tests, " +
                    "${result.successfulTestCount} successful, " +
                    "${result.failedTestCount} failures, " +
                    "${result.skippedTestCount} skipped, " +
                    "${result.durationSeconds()} runtime"
            )
    }

    override fun beforeTest(testDescriptor: TestDescriptor) { }

    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
        output.style(Style.Normal)
            .text(" > ")
            .style(deriveResultStyle(result))
            .text(testDescriptor.name)
            .text(" (")
            .text(if (result.skipped()) "SKIPPED" else result.durationSeconds())
            .println(")")
    }

    private fun deriveResultStyle(result: TestResult): Style = when {
        result.failed() -> Style.Failure
        result.skipped() -> Style.ProgressStatus
        else -> Style.Success
    }

    private fun TestResult.durationSeconds(): String = String.format("%.2fs", (endTime - startTime) / 1000.0)

    private fun TestResult.failed(): Boolean = failedTestCount > 0
    private fun TestResult.skipped(): Boolean = skippedTestCount > 0
}
