package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.generator.MapStyle;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

@Getter
public class StyleOptions {
    @CommandLine.ArgGroup(heading = "Options to create a custom map style%n", exclusive = false)
    @Setter
    private @Nullable CustomStyleOptions customStyleOptions;
    private MapStyle.@Nullable Predefined predefinedMapStyle;

    @CommandLine.Option(names = "--style", order = 50, description = "Style for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setPredefinedMapStyle(MapStyle.Predefined predefinedMapStyle) {
        if (this.predefinedMapStyle != null) {
            throw new IllegalStateException("Map style is already set");
        }

        this.predefinedMapStyle = predefinedMapStyle;
    }
}
