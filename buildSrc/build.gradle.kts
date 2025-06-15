plugins {
    `kotlin-dsl`
}

group = "com.faforever.neroxis"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.adarshr:gradle-test-logger-plugin:4.0.0")
    implementation("com.github.johnrengelman.shadow:com.github.johnrengelman.shadow.gradle.plugin:7.1.2")
    implementation("org.beryx.jlink:org.beryx.jlink.gradle.plugin:3.1.1")
}

kotlin {
    jvmToolchain(21)
}