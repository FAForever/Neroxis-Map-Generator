package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

public class CustomStyleGenerator extends StyleGenerator {

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                getGeneratorParameters().terrainGenerator().getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                getGeneratorParameters().resourceGenerator().getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                getGeneratorParameters().propGenerator().getGeneratorSupplier().get());
    }
}
