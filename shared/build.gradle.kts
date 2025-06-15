plugins {
    id("com.faforever.neroxis.conventions-java")
    id("antlr")
    id("me.champeau.jmh") version "0.7.3"
}

dependencies {
    antlr("org.antlr:antlr4:4.13.2")

    val avajeVersion = 3.5
    implementation("io.avaje:avaje-jsonb:$avajeVersion")
    annotationProcessor("io.avaje:avaje-jsonb-generator:$avajeVersion")
}

jmh {
    warmupIterations = 2
    iterations = 5
    fork = 1
}

tasks.jar {
    excludes += "/source_images/*"
}

tasks.register<AntlrTask>("generateLexerSource") {
    source = fileTree("src/main/antlr/com/faforever/neroxis/lua/LuaLexer.g4")
    outputDirectory = file("build/generated-src/antlr/main/com/faforever/neroxis/lua")
    arguments = listOf("-package", "com.faforever.neroxis.lua", "-visitor", "-no-listener")
}

tasks.named<AntlrTask>("generateGrammarSource") {
    dependsOn += tasks.named("generateLexerSource")
    source = fileTree("src/main/antlr/com/faforever/neroxis/lua/LuaParser.g4")
    arguments = listOf("-lib", "build/generated-src/antlr/main/com/faforever/neroxis/lua", "-package", "com.faforever.neroxis.lua", "-visitor", "-no-listener")
}