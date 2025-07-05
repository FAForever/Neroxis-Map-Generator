plugins {
    id("com.faforever.neroxis.conventions-java")
    id("com.faforever.neroxis.conventions-application")
}

application {
    mainModule = "com.faforever.neroxis.toolsuite"
    mainClass = "com.faforever.neroxis.toolsuite.MapToolSuite"
}

dependencies {
    implementation(project(":shared"))
}

jlink {
    launcher {
        name = "neroxis-toolsuite"
    }
}