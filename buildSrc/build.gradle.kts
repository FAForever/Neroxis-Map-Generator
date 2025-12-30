plugins {
    `kotlin-dsl`
}

group = "com.faforever.neroxis"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.adarshr:gradle-test-logger-plugin:4.0.0")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.2.2")
    implementation("org.beryx.jlink:org.beryx.jlink.gradle.plugin:3.1.4-rc")
}

kotlin {
    jvmToolchain(25)
}