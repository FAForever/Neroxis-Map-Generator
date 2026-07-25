package com.faforever.neroxis.generator.resource;

public class LowMexResourceGenerator extends BasicResourceGenerator {

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


