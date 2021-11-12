package org.gradle.dependency.constrain

import groovy.transform.CompileStatic
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

@CompileStatic
abstract class BaseFunctionalTest extends Specification {
    @TempDir
    File projectDir

    protected void writeString(File file, String string) throws IOException {
        try (Writer writer = new FileWriter(file, true)) {
            writer.write(string);
        }
    }

    protected void applyConstraintPlugin() {
        writeString(new File(projectDir, "settings.gradle"),
            """
            plugins {
                id('org.gradle.dependency.constrain')
            }""".stripMargin()
        )
    }

    GradleRunner createGradleRunner() {
        GradleRunner runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withProjectDir(projectDir)
        return runner
    }

    protected BuildResult succeed(String... args) {
        GradleRunner runner = createGradleRunner()
        List<String> arguments = args.toList()
        arguments.add("--stacktrace")
        return runner.withArguments(arguments).build()
    }
}
