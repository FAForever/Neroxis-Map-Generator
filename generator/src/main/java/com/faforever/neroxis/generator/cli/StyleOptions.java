package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.generator.MapStyle;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

@Getter
public class StyleOptions {
    @CommandLine.ArgGroup(heading = "Options to create a custom map style%n", exclusive = false)
    @Setter
    private CustomStyleOptions customStyleOptions;
    private MapStyle mapStyle;

    @CommandLine.Option(names = "--style", order = 50, description = "Style for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setMapStyle(MapStyle mapStyle) {
        if (this.mapStyle != null) {
            throw new IllegalStateException("Map style is already set");
        }

        this.mapStyle = mapStyle;
    }
}
