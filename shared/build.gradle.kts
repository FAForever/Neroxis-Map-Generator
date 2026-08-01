plugins {
    id("com.faforever.neroxis.conventions-java")
    id("antlr")
    id("me.champeau.jmh") version "0.7.3"
}

dependencies {
    antlr("org.antlr:antlr4:4.13.2")
}

jmh {
    warmupIterations = 5
    iterations = 5
    fork = 2
    jvmArgs.addAll("--add-modules=jdk.incubator.vector", "-XX:+UseCompactObjectHeaders")
    resultFormat = "JSON"
    resultsFile = layout.buildDirectory.file("reports/jmh/results.json")
    if (project.hasProperty("jmhIncludes")) {
        includes.add(project.property("jmhIncludes") as String)
    }
    if (project.hasProperty("jmhSizes")) {
        benchmarkParameters.put("size", objects.listProperty<String>().value((project.property("jmhSizes") as String).split(",")))
    }
}

tasks.named<JavaCompile>("compileJmhJava") {
    options.compilerArgs.addAll(listOf("--add-modules", "jdk.incubator.vector"))
}

tasks.register<AntlrTask>("generateLexerSource") {
    source = fileTree("src/main/antlr/com/faforever/neroxis/lua/LuaLexer.g4")
    outputDirectory = file("build/generated-src/antlr/main/com/faforever/neroxis/lua")
    arguments = listOf("-package", "com.faforever.neroxis.lua", "-visitor", "-no-listener")
}

tasks.named<AntlrTask>("generateGrammarSource") {
    dependsOn += tasks.named("generateLexerSource")
    source = fileTree("src/main/antlr/com/faforever/neroxis/lua/LuaParser.g4")
    arguments = listOf(
        "-lib",
        "build/generated-src/antlr/main/com/faforever/neroxis/lua",
        "-package",
        "com.faforever.neroxis.lua",
        "-visitor",
        "-no-listener"
    )
}