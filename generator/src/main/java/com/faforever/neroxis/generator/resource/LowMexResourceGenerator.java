package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.generator.ParameterConstraints;

public class LowMexResourceGenerator extends BasicResourceGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mapSizes(384, 768)
                                   .spawnCount(0, 4)
                                   .build();
    }

    @Override
    protected int getMexCount() {
        int mexCount;
        int mapSize = generatorParameters.mapSize();
        int spawnCount = generatorParameters.spawnCount();
        float mexMultiplier = 1f;
        if (spawnCount <= 2) {
            mexCount = (int) (10 + 2 * resourceDensity);
        } else if (spawnCount <= 4) {
            mexCount = (int) (8 + 6 * resourceDensity);
        } else {
            mexCount = (int) (6 + 4 * resourceDensity);
        }
        if (mapSize < 512) {
            mexMultiplier = .9f;
        }
        mexCount = StrictMath.round(mexCount * mexMultiplier);
        return mexCount * spawnCount;
    }
}


