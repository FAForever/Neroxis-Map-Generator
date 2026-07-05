package com.faforever.neroxis.generator.style;


import com.faforever.neroxis.generator.MapStyle;
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

    private final MapStyle.Custom mapStyle;

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        TerrainGenerator terrainGenerator = mapStyle.terrainStyle().getGeneratorSupplier().get();
        return WeightedOptionsWithFallback.of(terrainGenerator);
    }

    @Override
    protected WeightedOptionsWithFallback<TextureGenerator> getTextureGeneratorOptions() {
        TextureGenerator textureGenerator = mapStyle.textureStyle().getGeneratorSupplier().get();
        return WeightedOptionsWithFallback.of(textureGenerator);
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        ResourceGenerator resourceGenerator = mapStyle.resourceStyle().getGeneratorSupplier().get();
        resourceGenerator.setResourceDensity(mapStyle.resourceDensity());
        return WeightedOptionsWithFallback.of(resourceGenerator);
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        PropGenerator propGenerator = mapStyle.propStyle().getGeneratorSupplier().get();
        propGenerator.setReclaimDensity(mapStyle.reclaimDensity());
        return WeightedOptionsWithFallback.of(propGenerator);
    }
}
