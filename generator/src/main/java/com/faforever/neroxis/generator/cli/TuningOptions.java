package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.generator.MapStyle;
import lombok.Getter;
import picocli.CommandLine;

@Getter
public class TuningOptions {
    @CommandLine.ArgGroup(order = 2, heading = "Options to set the generated map visibility%n")
    private VisibilityOptions visibilityOptions;
    @CommandLine.ArgGroup(order = 1, heading = "Options to manually tune map generation%n", exclusive = false)
    private ParameterOptions parameterOptions;
    private MapStyle mapStyle;

    @CommandLine.Option(names = "--style", order = 8, description = "Style for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setMapStyle(MapStyle mapStyle) {
        if (this.mapStyle != null) {
            throw new IllegalStateException("Map style is already set");
        }

        this.mapStyle = mapStyle;
    }
}
