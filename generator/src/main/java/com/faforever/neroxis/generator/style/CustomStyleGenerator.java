package com.faforever.neroxis.generator.style;


import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.cli.CustomStyleOptions;
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

    public CustomStyleGenerator(CustomStyleOptions customStyleOptions) {
        terrainGenerator = customStyleOptions.getTerrainStyle().getGeneratorSupplier().get();
        textureGenerator = customStyleOptions.getTextureStyle().getGeneratorSupplier().get();
        resourceGenerator = customStyleOptions.getResourceStyle().getGeneratorSupplier().get();
        propGenerator = customStyleOptions.getPropStyle().getGeneratorSupplier().get();
        resourceGenerator.setResourceDensity(customStyleOptions.getResourceDensity());
        propGenerator.setReclaimDensity(customStyleOptions.getReclaimDensity());
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
