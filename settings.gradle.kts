pluginManagement {
    plugins {
        id("com.github.johnrengelman.shadow") version "7.1.0"
        id("com.diffplug.spotless") version "6.0.0"
    }
}
plugins {
    id("com.gradle.enterprise").version("3.7.2")
    id("com.gradle.enterprise.gradle-enterprise-conventions-plugin").version("0.7.4")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

gradleEnterprise {
    buildScan {
        val buildUrl = System.getenv("BUILD_URL") ?: ""
        if (buildUrl.isNotBlank()) {
            link("Build URL", buildUrl)
        }
    }
}

apply(from = "gradle/build-cache-configuration.settings.gradle.kts")

rootProject.name = "gradle-dependency-constrain"

includeBuild("constrain-build-logic")
include("constrain-lib")
include("constrain-plugin")

enableFeaturePreview("VERSION_CATALOGS")
