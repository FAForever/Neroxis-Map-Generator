package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.TextureGenerator;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import lombok.Setter;

@Setter
public class CustomStyleGenerator extends StyleGenerator {
    private com.faforever.neroxis.generator.TerrainGenerator terrainGenerator;
    private TextureGenerator textureGenerator;
    private com.faforever.neroxis.generator.ResourceGenerator resourceGenerator;
    private com.faforever.neroxis.generator.PropGenerator propGenerator;

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                terrainGenerator.getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<com.faforever.neroxis.generator.texture.TextureGenerator> getTextureGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                textureGenerator.getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                resourceGenerator.getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                propGenerator.getGeneratorSupplier().get());
    }
}
