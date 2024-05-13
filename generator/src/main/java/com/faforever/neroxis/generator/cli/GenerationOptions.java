package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.generator.MapStyle;
import lombok.Getter;
import picocli.CommandLine;

@Getter
public class GenerationOptions {
    @CommandLine.ArgGroup(order = 3, heading = "Options to set the generated map visibility%n")
    private VisibilityOptions visibilityOptions;
    @CommandLine.ArgGroup(order = 4, heading = "Options to set the generated map symmetry%n")
    private CasualOptions casualOptions = new CasualOptions();
}
