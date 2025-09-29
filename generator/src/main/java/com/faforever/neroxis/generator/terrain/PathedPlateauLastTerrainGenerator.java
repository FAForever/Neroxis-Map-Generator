package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.mask.MapMaskMethods;

public abstract class PathedPlateauLastTerrainGenerator extends BasicLastTerrainGenerator {
    @Override
    protected void plateausSetup() {
        int mapSize = map.getSize();
        int maxStepSize = mapSize / 128;
        int maxMiddlePoints = 16;
        int numPaths = (int) (12 * plateauDensity) / symmetrySettings.spawnSymmetry().getNumSymPoints();
        int bound = 0;
        plateaus.setSize(mapSize + 1);

        MapMaskMethods.pathInCenterBounds(random.nextLong(), plateaus, maxStepSize, numPaths, maxMiddlePoints, bound,
                                          (float) (StrictMath.PI / 2));
        plateaus.inflate(mapSize / 256).setSize(mapSize / 4);
        plateaus.dilute(.5f, 4).setSize(mapSize + 1);
        plateaus.blur(12);
    }
}
