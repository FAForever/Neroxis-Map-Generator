package com.faforever.neroxis.generator.cli;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

@Getter
public class GenerationOptions {
    @CommandLine.ArgGroup
    private @Nullable VisibilityOptions visibilityOptions;
    @CommandLine.ArgGroup(exclusive = false)
    private CasualOptions casualOptions = new CasualOptions();
}
