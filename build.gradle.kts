subprojects {
    group = "org.gradle.dependency.constrain"
    version = "0.1"

    repositories {
        mavenCentral()
    }
    plugins.withId("java-library") {
        dependencies {
            "testImplementation"("org.junit.jupiter:junit-jupiter:5.7.2")
            "testImplementation"("org.spockframework:spock-core:2.0-groovy-3.0")
        }

        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    tasks.withType<Test> {
        // Use JUnit Platform for unit tests.
        useJUnitPlatform()
    }
}

