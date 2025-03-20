package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.resource.RiversAndOceansMexResourceGenerator;
import com.faforever.neroxis.generator.terrain.FloodedMultiLevelTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

public class FloodedMultiLevelStyleGenerator extends StyleGenerator {
    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new FloodedMultiLevelTerrainGenerator());
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new RiversAndOceansMexResourceGenerator());
    }
}
