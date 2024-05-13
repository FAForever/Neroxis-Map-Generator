package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.generator.MapStyle;
import lombok.Getter;
import picocli.CommandLine;

@Getter
public class StyleOptions {
    @CommandLine.ArgGroup(order = 2, heading = "Options to create a custom map style%n", exclusive = false)
    private CustomStyleOptions customStyleOptions;
    private MapStyle mapStyle;

    @CommandLine.Option(names = "--style", order = 6, description = "Style for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setMapStyle(MapStyle mapStyle) {
        if (this.mapStyle != null) {
            throw new IllegalStateException("Map style is already set");
        }

        this.mapStyle = mapStyle;
    }
}
