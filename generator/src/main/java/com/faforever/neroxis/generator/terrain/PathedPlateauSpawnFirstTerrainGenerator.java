package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.mask.MapMaskMethods;

public abstract class PathedPlateauSpawnFirstTerrainGenerator extends BasicSpawnFirstTerrainGenerator {
    @Override
    protected void plateausSetup() {
        int mapSize = map.getSize();
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 16;
        int numPaths = (int) (12 * generatorParameters.plateauDensity()) / symmetrySettings.getSpawnSymmetry()
                                                                                           .getNumSymPoints();
        int bound = 0;
        plateaus.setSize(mapSize + 1);

        MapMaskMethods.pathInCenterBounds(random.nextLong(), plateaus, maxStepSize, numPaths, maxMiddlePoints, bound,
                                          (float) (StrictMath.PI / 2));
        plateaus.inflate(mapSize / 256f).setSize(mapSize / 4);
        plateaus.dilute(.5f, 4).setSize(mapSize + 1);
        plateaus.blur(12);
    }
}
