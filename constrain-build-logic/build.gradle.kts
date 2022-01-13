plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    implementation(platform(libs.jackson.platform))
    implementation(libs.jackson.yaml)
}
