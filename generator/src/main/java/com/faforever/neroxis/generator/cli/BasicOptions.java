package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.cli.MultipleMapSizeConverter;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

import java.util.SplittableRandom;

@Getter
@Setter
public class BasicOptions {
    @CommandLine.Option(names = "--seed", order = 3, description = "Seed for the generated map")
    private Long seed = new SplittableRandom().nextLong();
    @CommandLine.Option(names = "--spawn-count", order = 5, defaultValue = "6", description = "Spawn count for the generated map", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private Integer spawnCount;
    @CommandLine.Option(names = "--num-teams", order = 6, defaultValue = "2", description = "Number of teams for the generated map (0 is no teams asymmetric)", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private Integer numTeams;
    @CommandLine.Option(names = "--map-size", order = 4, defaultValue = "512", description = "Generated map size, can be specified in oGrids (e.g 512) or km (e.g 10km)", showDefaultValue = CommandLine.Help.Visibility.ALWAYS, converter = MultipleMapSizeConverter.class)
    private Integer mapSize;
}
