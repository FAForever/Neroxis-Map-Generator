plugins {
    `kotlin-dsl`
}

group = "com.faforever.neroxis"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.adarshr:gradle-test-logger-plugin:4.0.0")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.4.2")
    implementation("org.beryx.jlink:org.beryx.jlink.gradle.plugin:3.1.4-rc")
    implementation("net.ltgt.errorprone:net.ltgt.errorprone.gradle.plugin:5.1.0")
    implementation("net.ltgt.nullaway:net.ltgt.nullaway.gradle.plugin:3.0.0")
}

kotlin {
    jvmToolchain(25)
}