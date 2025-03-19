package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.terrain.MultiLevelTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

public class MultiLevelStyleGenerator extends StyleGenerator {
    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new MultiLevelTerrainGenerator());
    }
}
