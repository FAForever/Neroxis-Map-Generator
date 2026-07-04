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
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CustomStyleGenerator extends StyleGenerator {

    private final TerrainStyle terrainStyle;
    private final TextureStyle textureStyle;
    private final ResourceStyle resourceStyle;
    private final PropStyle propStyle;
    private final float resourceDensity;
    private final float reclaimDensity;

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        TerrainGenerator terrainGenerator = terrainStyle.getGeneratorSupplier().get();
        return WeightedOptionsWithFallback.of(terrainGenerator);
    }

    @Override
    protected WeightedOptionsWithFallback<TextureGenerator> getTextureGeneratorOptions() {
        TextureGenerator textureGenerator = textureStyle.getGeneratorSupplier().get();
        return WeightedOptionsWithFallback.of(textureGenerator);
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        ResourceGenerator resourceGenerator = resourceStyle.getGeneratorSupplier().get();
        resourceGenerator.setResourceDensity(resourceDensity);
        return WeightedOptionsWithFallback.of(resourceGenerator);
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        PropGenerator propGenerator = propStyle.getGeneratorSupplier().get();
        propGenerator.setReclaimDensity(reclaimDensity);
        return WeightedOptionsWithFallback.of(propGenerator);
    }
}
