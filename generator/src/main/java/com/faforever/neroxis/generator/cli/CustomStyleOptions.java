package com.faforever.neroxis.generator.cli;


import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.generator.util.serial.PropStyle;
import com.faforever.neroxis.generator.util.serial.ResourceStyle;
import com.faforever.neroxis.generator.util.serial.TerrainStyle;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import static picocli.CommandLine.Option;

@Getter
@Setter
public class CustomStyleOptions {
    @Option(
            names = {"--texture-style", "--biome"},
            description = "Texture style to use for the generated map. Values: ${COMPLETION-CANDIDATES}"
    )
    private @Nullable BiomeName biomeName;
    @Option(
            names = "--terrain-style",
            order = 29,
            description = "Terrain style to use for the generated map. Values: ${COMPLETION-CANDIDATES}"
    )
    private @Nullable TerrainStyle terrainStyle;
    @Option(
            names = "--resource-style",
            order = 29,
            description = "Resource style to use for the generated map. Values: ${COMPLETION-CANDIDATES}"
    )
    private @Nullable ResourceStyle resourceStyle;
    @Option(
            names = "--prop-style",
            order = 29,
            description = "Prop style to use for the generated map. Values: ${COMPLETION-CANDIDATES}"
    )
    private @Nullable PropStyle propStyle;
    @Option(
            names = "--reclaim-density",
            order = 29,
            description = "Reclaim density for the generated map. Min: 0 Max: 1",
            converter = BinnedDensityConverter.class
    )
    private @Nullable Float reclaimDensity;
    @Option(
            names = "--resource-density",
            order = 29,
            description = "Resource density for the generated map. Min: 0 Max: 1",
            converter = BinnedDensityConverter.class
    )
    private @Nullable Float resourceDensity;
}
