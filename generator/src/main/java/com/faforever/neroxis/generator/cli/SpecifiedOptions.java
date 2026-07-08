package com.faforever.neroxis.generator.cli;

import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

@Getter
@Setter
public class SpecifiedOptions {
    @CommandLine.ArgGroup(exclusive = false)
    private BasicOptions basicOptions = new BasicOptions();
    @CommandLine.ArgGroup
    private GenerationOptions generationOptions = new GenerationOptions();
    @CommandLine.Option(
            names = "--num-to-generate", order = 2, defaultValue = "1", description = "Number of maps to create"
    )
    private int numToGenerate = 1;
}
