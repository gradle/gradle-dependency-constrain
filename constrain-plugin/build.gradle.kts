import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

val relocateShadowJar = tasks.register<ConfigureShadowRelocation>("relocateShadowJar") {
    // Enable package relocation in resulting shadow jar
    prefix = "$pluginGroup.shadow"
    target = shadowJarTask.get() // This compiles! 🤯
}

val shadowJarTask: TaskProvider<ShadowJar> = tasks.named<ShadowJar>("shadowJar") {
    dependsOn(relocateShadowJar)
    archiveClassifier.set("")
    configurations = listOf(shadowImplementation)
}

configurations {
    artifacts {
        runtimeElements(shadowJarTask)
        apiElements(shadowJarTask)
    }
}

// Disabling default jar task as it is overridden by shadowJar
tasks.named("jar").configure {
    enabled = false
}

tasks.whenTaskAdded {
    if (name == "publishPluginJar" || name == "generateMetadataFileForPluginMavenPublication") {
        dependsOn(tasks.named("shadowJar"))
    }
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
