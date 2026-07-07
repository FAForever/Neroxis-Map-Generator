package com.faforever.neroxis.generator.cli;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

@Getter
@Setter
public class TopLevelOptions {
    @CommandLine.Option(
            names = "--map-name",
            order = 1,
            description = "Name of map to recreate. Must be of the form neroxis_map_generator_version_seed_options, if present other parameter options will be ignored"
    )
    private @Nullable String specifiedMapName;

    @CommandLine.ArgGroup(exclusive = false)
    private SpecifiedOptions specifiedOptions = new SpecifiedOptions();
}
