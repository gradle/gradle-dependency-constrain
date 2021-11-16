pluginManagement {
    plugins {
        id("com.github.johnrengelman.shadow") version "7.1.0"
    }
}

rootProject.name = "gradle-dependency-constrain"
include("constrain-lib")
include("constrain-plugin")
