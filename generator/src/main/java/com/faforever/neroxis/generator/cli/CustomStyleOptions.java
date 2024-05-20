package com.faforever.neroxis.generator.cli;


import com.faforever.neroxis.generator.PropGeneratorSupplier;
import com.faforever.neroxis.generator.ResourceGeneratorSupplier;
import com.faforever.neroxis.generator.TerrainGeneratorSupplier;
import com.faforever.neroxis.generator.TextureGeneratorSupplier;
import lombok.Getter;
import picocli.CommandLine;

import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Getter
public class CustomStyleOptions {
    @Spec
    CommandLine.Model.CommandSpec spec;
    private TextureGeneratorSupplier textureGenerator;
    private TerrainGeneratorSupplier terrainGenerator;
    private ResourceGeneratorSupplier resourceGenerator;
    private PropGeneratorSupplier propGenerator;

    @Option(names = "--biome", description = "Texture biome for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setTextureGenerator(TextureGeneratorSupplier textureGenerator) {
        this.textureGenerator = textureGenerator;
    }

    @Option(names = "--terrain-generator", order = 29, description = "Terrain generator to use for generating the map. Values: ${COMPLETION-CANDIDATES}")
    public void setTerrainGenerator(TerrainGeneratorSupplier terrainGenerator) {
        this.terrainGenerator = terrainGenerator;
    }

    @Option(names = "--resource-generator", order = 29, description = "Resource generator to use for generating the map. Values: ${COMPLETION-CANDIDATES}")
    public void setResourceGenerator(ResourceGeneratorSupplier resourceGenerator) {
        this.resourceGenerator = resourceGenerator;
    }

    @Option(names = "--prop-generator", order = 29, description = "Prop generator to use for generating the map. Values: ${COMPLETION-CANDIDATES}")
    public void setPropGenerator(PropGeneratorSupplier propGenerator) {
        this.propGenerator = propGenerator;
    }
}
