plugins {
    `java-library`
    jacoco
    id("com.adarshr.test-logger")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jspecify:jspecify:1.0.0")

    val lombokVersion = "1.18.38"
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    val picocliVersion = "4.7.6"
    implementation("info.picocli:picocli:$picocliVersion")
    annotationProcessor("info.picocli:picocli-codegen:$picocliVersion")

    val junitVersion = "5.13.1"
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    systemProperties = mapOf("junit.jupiter.execution.parallel.enabled" to true)
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
}

tasks.withType(JavaCompile::class.java).configureEach {
    options.compilerArgs.add("-parameters")
}

testlogger {
    showPassed = false
}