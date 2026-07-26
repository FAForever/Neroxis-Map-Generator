plugins {
    application
    id("com.gradleup.shadow")
    id("org.beryx.jlink")
}

val generatorVersion: String = findProperty("generatorVersion") as String

jlink {
    options.addAll("--strip-debug", "--compress", "zip-9", "--no-header-files", "--no-man-pages")
    jpackage {
        if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
            installerOptions = listOf(
                "--win-per-user-install", "--win-dir-chooser", "--win-menu", "--win-shortcut", "--win-shortcut-prompt"
            )
            imageOptions = listOf("--win-console")
        }

        jvmArgs = listOf(
            "-XX:+UseCompactObjectHeaders",
            "-XX:AOTCache=runtime/bin/neroxis-${project.name}.aot"
        )

        if (generatorVersion != "snapshot") {
            appVersion = generatorVersion
        }
    }
    launcher {
        name = "neroxis-${project.name}"
        jvmArgs = listOf(
            "-XX:+UseCompactObjectHeaders",
            "-XX:AOTCache=neroxis-${project.name}.aot"
        )
        val templatesDir =
            rootDir.resolve("buildSrc").resolve("src").resolve("main").resolve("resources").resolve("templates")
        unixScriptTemplate = templatesDir.resolve("unixScriptTemplate.txt")
        windowsScriptTemplate = templatesDir.resolve("windowsScriptTemplate.txt")
    }
}

tasks.jar {
    manifest {
        attributes["Implementation-Version"] = generatorVersion
    }
    duplicatesStrategy = DuplicatesStrategy.WARN
}