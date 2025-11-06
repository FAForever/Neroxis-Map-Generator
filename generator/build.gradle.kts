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
    implementation("commons-codec:commons-codec:1.20.0")
}

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = "Neroxis Map Generator"
    }
}

tasks.shadowJar {
    archiveFileName = "NeroxisGen_${properties["generatorVersion"]}.jar"
}