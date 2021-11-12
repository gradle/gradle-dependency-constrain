plugins {
    groovy
    `java-library-distribution`
}

dependencies {
    compileOnly(gradleApi())
    testImplementation(gradleApi())
}
