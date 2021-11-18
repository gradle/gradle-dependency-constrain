import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.jar.JarFile

plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`
    // Apply the Groovy plugin to add support for Groovy
    groovy
    `maven-publish`
    id("com.github.johnrengelman.shadow")
}

val pluginGroup = group

val shadowImplementation: Configuration by configurations.creating
configurations["compileOnly"].extendsFrom(shadowImplementation)
configurations["testImplementation"].extendsFrom(shadowImplementation)

dependencies {
    shadowImplementation(project(":constrain-lib"))
}

tasks.named("test").configure {
    dependsOn("publishToMavenLocal")
}

tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.from(shadowImplementation)
}

val shadowJarTask: TaskProvider<ShadowJar> = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    configurations = listOf(shadowImplementation)
    doFirst {
        shadowImplementation.resolvedConfiguration.files.forEach { jar ->
            JarFile(jar).use { jf ->
                jf.entries().iterator().forEach { entry ->
                    if (entry.name.endsWith(".class") && entry.name != "module-info.class") {
                        val packageName =
                            entry
                                .name
                                .substring(0..entry.name.lastIndexOf('/'))
                                .replace('/', '.')
                        relocate(packageName, "${pluginGroup}.shadow.$packageName")
                    }
                }
            }
        }
    }
}

configurations.archives.get().artifacts.clear()
configurations {
    artifacts {
        runtimeElements(shadowJarTask)
        apiElements(shadowJarTask)
        archives(tasks.shadowJar)
    }
}

// Add the shadow JAR to the runtime consumable configuration
configurations.apiElements.get().artifacts.clear()
configurations.apiElements.get().outgoing.artifact(tasks.shadowJar)
configurations.runtimeElements.get().outgoing.artifacts.clear()
configurations.runtimeElements.get().outgoing.artifact(tasks.shadowJar)

// Disabling default jar task as it is overridden by shadowJar
tasks.named("jar").configure {
    enabled = false
}

gradlePlugin {
    // Define the plugin
    val constrain by plugins.creating {
        id = "org.gradle.dependency.constrain"
        implementationClass = "org.gradle.dependency.constrain.GradleDependencyConstrainPlugin"
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") { }

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations["functionalTestImplementation"].extendsFrom(
    configurations["testImplementation"]
)

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

tasks.check {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

// Need to move publishing configuration into afterEvaluate {}
// to override changes done by "com.gradle.plugin-publish" plugin in afterEvaluate {} block
// See PublishPlugin class for details
afterEvaluate {
    publishing {
        publications {
            withType<MavenPublication> {
                // Special workaround to publish shadow jar instead of normal one. Name to override peeked here:
                // https://github.com/gradle/gradle/blob/master/subprojects/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/MavenPluginPublishPlugin.java#L73
                if (name == "pluginMaven") {
                    setArtifacts(
                        listOf(
                            shadowJarTask.get()
                        )
                    )
                }
            }
        }
    }
}
