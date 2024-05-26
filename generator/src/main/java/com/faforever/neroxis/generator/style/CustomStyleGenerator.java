package com.faforever.neroxis.generator.style;


import com.faforever.neroxis.generator.PropStyle;
import com.faforever.neroxis.generator.ResourceStyle;
import com.faforever.neroxis.generator.TerrainStyle;
import com.faforever.neroxis.generator.TextureStyle;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.texture.TextureGenerator;
import lombok.Setter;

@Setter
public class CustomStyleGenerator extends StyleGenerator {
    private TerrainStyle terrainStyle;
    private TextureStyle textureStyle;
    private ResourceStyle resourceStyle;
    private PropStyle propStyle;

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                terrainStyle.getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<TextureGenerator> getTextureGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                textureStyle.getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                resourceStyle.getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                propStyle.getGeneratorSupplier().get());
    }
}
