package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.mask.MapMaskMethods;

public abstract class PathedLastTerrainGenerator extends BasicLastTerrainGenerator {

    @Override
    protected void initRamps() {
        int mapSize = map.getSize();
        int maxStepSize = mapSize / 128;
        int maxMiddlePoints = 2;
        int numPaths = (int) (rampDensity * 20) / symmetrySettings.terrainSymmetry().getNumSymPoints();
        int bound = mapSize / 4;
        ramps.setSize(mapSize + 1);

        MapMaskMethods.pathInEdgeBounds(random.split(), ramps, maxStepSize, numPaths, maxMiddlePoints, bound,
                                        (float) (StrictMath.PI / 2));
        MapMaskMethods.pathInCenterBounds(random.split(), ramps, maxStepSize, numPaths / 2, maxMiddlePoints, bound,
                                          (float) (StrictMath.PI / 2));

        ramps.subtract(connections.copy().inflate(32))
             .inflate(maxStepSize / 2)
             .multiply(plateaus.copy().outline())
             .add(connections.copy().inflate(maxStepSize / 2).multiply(plateaus.copy().outline()))
             .subtract(mountains)
             .inflate(8);
    }
}


