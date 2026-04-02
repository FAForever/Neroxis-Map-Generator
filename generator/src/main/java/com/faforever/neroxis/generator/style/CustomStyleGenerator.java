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
    private final TerrainGenerator terrainGenerator;
    private final TextureGenerator textureGenerator;
    private final ResourceGenerator resourceGenerator;
    private final PropGenerator propGenerator;

    public CustomStyleGenerator(TerrainStyle terrainStyle, TextureStyle textureStyle, ResourceStyle resourceStyle,
                                PropStyle propStyle, float resourceDensity, float reclaimDensity) {
        terrainGenerator = terrainStyle.getGeneratorSupplier().get();
        textureGenerator = textureStyle.getGeneratorSupplier().get();
        resourceGenerator = resourceStyle.getGeneratorSupplier().get();
        propGenerator = propStyle.getGeneratorSupplier().get();
        resourceGenerator.setResourceDensity(resourceDensity);
        propGenerator.setReclaimDensity(reclaimDensity);
    }

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(terrainGenerator);
    }

    @Override
    protected WeightedOptionsWithFallback<TextureGenerator> getTextureGeneratorOptions() {
        return WeightedOptionsWithFallback.of(textureGenerator);
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(resourceGenerator);
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(propGenerator);
    }
}
