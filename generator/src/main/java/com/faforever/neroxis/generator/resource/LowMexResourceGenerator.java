package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.generator.ParameterConstraints;

public strictfp class LowMexResourceGenerator extends BasicResourceGenerator {

    public LowMexResourceGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                                                   .mexDensity(0f, .25f)
                                                   .mapSizes(384, 768)
                                                   .spawnCount(0, 4)
                                                   .build();
    }

    @Override
    protected int getMexCount() {
        int mexCount;
        int mapSize = generatorParameters.getMapSize();
        int spawnCount = generatorParameters.getSpawnCount();
        float mexDensity = parameterConstraints.getMexDensityRange().normalize(generatorParameters.getMexDensity());
        float mexMultiplier = 1f;
        if (spawnCount <= 2) {
            mexCount = (int) (10 + 2 * mexDensity);
        } else if (spawnCount <= 4) {
            mexCount = (int) (8 + 6 * mexDensity);
        } else {
            mexCount = (int) (6 + 4 * mexDensity);
        }
        if (mapSize < 512) {
            mexMultiplier = .9f;
        }
        mexCount *= mexMultiplier;
        return mexCount * spawnCount;
    }
}


