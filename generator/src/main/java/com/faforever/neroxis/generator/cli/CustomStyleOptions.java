package com.faforever.neroxis.generator.cli;


import com.faforever.neroxis.generator.PropGenerator;
import com.faforever.neroxis.generator.ResourceGenerator;
import com.faforever.neroxis.generator.TerrainGenerator;
import com.faforever.neroxis.generator.TextureGenerator;
import lombok.Getter;
import picocli.CommandLine;

import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Getter
public class CustomStyleOptions {
    @Spec
    CommandLine.Model.CommandSpec spec;
    private TextureGenerator textureGenerator;
    private TerrainGenerator terrainGenerator;
    private ResourceGenerator resourceGenerator;
    private PropGenerator propGenerator;

    @Option(names = "--biome", description = "Texture biome for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setTextureGenerator(TextureGenerator textureGenerator) {
        this.textureGenerator = textureGenerator;
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
