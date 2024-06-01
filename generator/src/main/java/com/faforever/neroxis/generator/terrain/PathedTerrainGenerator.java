package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.MapMaskMethods;

public abstract class PathedTerrainGenerator extends BasicTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
    }

    @Override
    protected void spawnTerrainSetup() {
        int mapSize = map.getSize();
        spawnPlateauMask.setSize(mapSize / 4);
        spawnPlateauMask.erode(.5f, 4).dilute(.5f, 8);
        spawnPlateauMask.erode(.5f).setSize(mapSize + 1);
        spawnPlateauMask.blur(4);

        spawnLandMask.setSize(mapSize / 4);
        spawnLandMask.erode(.25f, mapSize / 128).dilute(.5f, 4);
        spawnLandMask.erode(.5f).setSize(mapSize + 1);
        spawnLandMask.blur(4);

        plateaus.subtract(spawnLandMask).add(spawnPlateauMask);
        land.add(spawnLandMask).add(spawnPlateauMask);

        ensureSpawnTerrain();

        mountains.multiply(land.copy().deflate(24));
    }

    @Override
    protected void initRamps() {
        int mapSize = map.getSize();
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 2;
        int numPaths = (int) (rampDensity * 20) / symmetrySettings.terrainSymmetry().getNumSymPoints();
        int bound = mapSize / 4;
        ramps.setSize(mapSize + 1);

        MapMaskMethods.pathInEdgeBounds(random.nextLong(), ramps, maxStepSize, numPaths, maxMiddlePoints, bound,
                                        (float) (StrictMath.PI / 2));
        MapMaskMethods.pathInCenterBounds(random.nextLong(), ramps, maxStepSize, numPaths / 2, maxMiddlePoints, bound,
                                          (float) (StrictMath.PI / 2));

        ramps.subtract(connections.copy().inflate(32))
             .inflate(maxStepSize / 2f)
             .multiply(plateaus.copy().outline())
             .add(connections.copy().inflate(maxStepSize / 2f).multiply(plateaus.copy().outline()))
             .subtract(mountains)
             .inflate(8);
    }
}


