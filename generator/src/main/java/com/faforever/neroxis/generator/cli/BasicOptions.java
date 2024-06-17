package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.cli.CLIUtils;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

import java.util.Random;

@Getter
@Setter
public class BasicOptions {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;
    @CommandLine.Option(names = "--seed", order = 3, description = "Seed for the generated map")
    private Long seed = new Random().nextLong();
    @CommandLine.Option(names = "--spawn-count", order = 5, defaultValue = "6", description = "Spawn count for the generated map", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private Integer spawnCount;
    @CommandLine.Option(names = "--num-teams", order = 6, defaultValue = "2", description = "Number of teams for the generated map (0 is no teams asymmetric)", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private Integer numTeams;
    private Integer mapSize;

    @CommandLine.Option(names = "--map-size", order = 4, defaultValue = "512", description = "Generated map size, can be specified in oGrids (e.g 512) or km (e.g 10km)", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    public void setMapSize(String mapSizeString) {
        this.mapSize = CLIUtils.convertMapSizeString(mapSizeString, CLIUtils.MapSizeStrictness.DISCRETE_64, spec);
    }

    public void setMapSize(int mapSize) {
        this.mapSize = mapSize;
    }
}
