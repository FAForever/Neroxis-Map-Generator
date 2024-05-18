package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.generator.PropGenerator;
import com.faforever.neroxis.generator.ResourceGenerator;
import com.faforever.neroxis.generator.TerrainGenerator;
import lombok.Getter;
import picocli.CommandLine;

import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Getter
@SuppressWarnings("unused")
public class CustomStyleOptions {
    @Spec
    CommandLine.Model.CommandSpec spec;
    private BiomeName biomeName;
    private TerrainGenerator terrainGenerator;
    private ResourceGenerator resourceGenerator;
    private PropGenerator propGenerator;

    @Option(names = "--biome", description = "Texture biome for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setBiomeName(BiomeName biome) {
        this.biomeName = biome;
    }

    @Option(names = "--terrain-generator", order = 29, description = "Terrain generator to use for generating the map. Values: ${COMPLETION-CANDIDATES}")
    public void setTerrainGenerator(TerrainGenerator terrainGenerator) {
        this.terrainGenerator = terrainGenerator;
    }

    @Option(names = "--resource-generator", order = 29, description = "Resource generator to use for generating the map. Values: ${COMPLETION-CANDIDATES}")
    public void setResourceGenerator(ResourceGenerator resourceGenerator) {
        this.resourceGenerator = resourceGenerator;
    }

    @Option(names = "--prop-generator", order = 29, description = "Prop generator to use for generating the map. Values: ${COMPLETION-CANDIDATES}")
    public void setPropGenerator(PropGenerator propGenerator) {
        this.propGenerator = propGenerator;
    }
}
