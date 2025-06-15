plugins {
    id("com.faforever.neroxis.conventions-java")
    id("com.faforever.neroxis.conventions-application")
}

application {
    mainClass = "com.faforever.neroxis.generator.MapGenerator"
    mainModule = "com.faforever.neroxis.generator"
}

dependencies {
    implementation(project(":shared"))
    implementation("commons-codec:commons-codec:1.18.0")
}

jlink {
    launcher {
        name = "neroxis-generator"
    }
}

tasks.shadowJar {
    val generatorVersion = properties["generatorVersion"]
    archiveFileName = "NeroxisGen_$generatorVersion.jar"
    manifest {
        attributes["Main-Class"] = "com.faforever.neroxis.map.generator.MapGenerator"
        attributes["Implementation-Version"] = generatorVersion
        attributes["Implementation-Title"] = "Neroxis Map Generator"
    }
    duplicatesStrategy = DuplicatesStrategy.WARN
}