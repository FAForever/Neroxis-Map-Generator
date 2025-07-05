plugins {
    application
    id("com.gradleup.shadow")
    id("org.beryx.jlink")
}

val generatorVersion: String = properties["generatorVersion"] as String

jlink {
    options.addAll("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
    enableCds()
    jpackage {
        if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
            installerOptions.addAll(
                listOf(
                    "--win-per-user-install",
                    "--win-dir-chooser",
                    "--win-menu",
                    "--win-shortcut"
                )
            )
            imageOptions.addAll(listOf("--win-console"))
        }
        if (generatorVersion != "snapshot") {
            appVersion = generatorVersion
        }
    }
}

tasks.jar {
    manifest {
        attributes["Implementation-Version"] = generatorVersion
    }
    duplicatesStrategy = DuplicatesStrategy.WARN
}