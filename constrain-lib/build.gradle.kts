plugins {
    groovy
    `java-library-distribution`
    id("gradlebuild.constrain-build-logic.base-plugin")
}

dependencies {
    compileOnly(gradleApi())
    implementation(platform(libs.jackson.platform))
    implementation("com.networknt:json-schema-validator:1.0.64")
    implementation(libs.jackson.parameter.names)
    implementation(libs.github.diff.utils)

    testImplementation(gradleApi())
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

val yamlToJson by tasks.creating(YamlToJsonConverter::class)
val yamlToJsonCheck by tasks.creating(YamlToJsonChecker::class)
tasks.withType<BaseYamlToJson>().configureEach {
    yamlFile.set(file("src/main/resources/schema/dependency-constraints-schema.yaml"))
    jsonFile.set(file("src/main/resources/schema/dependency-constraints-schema.json"))
}

tasks.named("spotlessCheck") {
    dependsOn(yamlToJsonCheck)
}
tasks.named("spotlessApply") {
    dependsOn(yamlToJson)
}
