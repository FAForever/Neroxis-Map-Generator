plugins {
    `java-library`
    id("com.adarshr.test-logger")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jspecify:jspecify:1.0.0")

    val lombokVersion = "1.18.42"
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    val picocliVersion = "4.7.7"
    implementation("info.picocli:picocli:$picocliVersion")
    annotationProcessor("info.picocli:picocli-codegen:$picocliVersion")

    val junitVersion = "6.0.1"
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    systemProperties = mapOf(
        "junit.jupiter.execution.parallel.enabled" to true,
        "junit.jupiter.execution.parallel.config.dynamic.max-pool-size-factor" to 4
    )
    modularity.inferModulePath = true
    maxHeapSize = "4096m"
}

tasks.withType(JavaCompile::class.java).configureEach {
    options.compilerArgs.add("-parameters")
}

testlogger {
    showPassed = false
}

tasks.jar {
    manifest {
        attributes["Implementation-Version"] = properties["generatorVersion"]
    }
    duplicatesStrategy = DuplicatesStrategy.WARN
}