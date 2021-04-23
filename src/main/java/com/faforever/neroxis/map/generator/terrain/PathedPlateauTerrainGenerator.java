package com.faforever.neroxis.map.generator.terrain;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;

public strictfp abstract class PathedPlateauTerrainGenerator extends BasicTerrainGenerator {

    @Override
    protected void plateausSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 16;
        int numPaths = (int) (12 * mapParameters.getPlateauDensity()) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = 0;
        plateaus.setSize(mapSize + 1);

        pathInCenterBounds(plateaus, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        plateaus.inflate(mapSize / 256f).setSize(mapSize / 4);
        plateaus.dilute(.5f, SymmetryType.TERRAIN, 4).setSize(mapSize + 1);
        plateaus.blur(12);
    }
}
