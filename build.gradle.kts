plugins {
    id("com.diffplug.spotless")
}

allprojects {
    apply(plugin = "com.diffplug.spotless")
}

subprojects {
    group = "org.gradle.dependency.constrain"
    version = "0.1"

    plugins.withId("java-library") {
        dependencies {
            "testImplementation"("org.junit.jupiter:junit-jupiter:5.7.2")
            "testImplementation"("org.spockframework:spock-core:2.0-groovy-3.0")
        }

        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            java {
                removeUnusedImports()
            }
        }
    }

    tasks.withType<Test> {
        // Use JUnit Platform for unit tests.
        useJUnitPlatform()
    }
}

