package com.faforever.neroxis.generator.style;


import com.faforever.neroxis.generator.*;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.texture.TextureGenerator;
import lombok.Setter;

@Setter
public class CustomStyleGenerator extends StyleGenerator {
    private TerrainGeneratorSupplier terrainGeneratorSupplier;
    private TextureGeneratorSupplier textureGeneratorSupplier;
    private ResourceGeneratorSupplier resourceGeneratorSupplier;
    private PropGeneratorSupplier propGeneratorSupplier;

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                terrainGeneratorSupplier.getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<TextureGenerator> getTextureGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                textureGeneratorSupplier.getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                resourceGeneratorSupplier.getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                propGeneratorSupplier.getGeneratorSupplier().get());
    }
}
