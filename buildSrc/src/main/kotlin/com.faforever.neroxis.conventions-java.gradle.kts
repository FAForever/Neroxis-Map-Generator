import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

plugins {
    `java-library`
    id("com.adarshr.test-logger")
    id("net.ltgt.errorprone")
    id("net.ltgt.nullaway")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

repositories {
    mavenCentral()
}

dependencies {
    errorprone("com.uber.nullaway:nullaway:0.13.4")
    errorprone("com.google.errorprone:error_prone_core:2.49.0")

    api("org.jspecify:jspecify:1.0.0")

    val avajeVersion = 3.9
    implementation("io.avaje:avaje-jsonb:$avajeVersion")
    annotationProcessor("io.avaje:avaje-jsonb-generator:$avajeVersion")

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

nullaway {
    jspecifyMode = true
    onlyNullMarked = true
}

tasks.test {
    useJUnitPlatform()
    systemProperties = mapOf(
        "junit.jupiter.execution.parallel.enabled" to true,
        "junit.jupiter.execution.parallel.config.dynamic.max-pool-size-factor" to 1
    )
    modularity.inferModulePath = true
    maxHeapSize = "4096m"
}

tasks.withType(JavaCompile::class.java).configureEach {
    options.compilerArgs.add("-parameters")
    options.errorprone {
        disableAllChecks = true
        nullaway {
            error()
            assertsEnabled = true
            treatGeneratedAsUnannotated = true
        }
    }
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