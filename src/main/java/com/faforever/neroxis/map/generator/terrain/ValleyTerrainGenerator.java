package com.faforever.neroxis.map.generator.terrain;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.mask.BooleanMask;

public strictfp class ValleyTerrainGenerator extends PathedPlateauTerrainGenerator {

    public ValleyTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.75f, 1f)
                .mountainDensity(.5f, 1)
                .mapSizes(512, 1024)
                .build();
    }

    @Override
    protected void landSetup() {
        land.setSize(map.getSize() + 1);
        land.invert();
    }

    @Override
    protected void mountainSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedMountainDensity = parameterConstraints.getMountainDensityRange().normalize(mapParameters.getMountainDensity());
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 8;
        int numPaths = (int) (8 + 8 * (1 - normalizedMountainDensity) / symmetrySettings.getTerrainSymmetry().getNumSymPoints());
        int bound = (int) (mapSize / 16 * (3 * (random.nextFloat() * .25f + normalizedMountainDensity * .75f) + 1));
        mountains.setSize(mapSize + 1);
        BooleanMask noMountains = new BooleanMask(mapSize + 1, random.nextLong(), symmetrySettings, "noMountains", true);

        pathInCenterBounds(noMountains, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        noMountains.setSize(mapSize / 4);
        noMountains.dilute(.5f, SymmetryType.SPAWN, (int) (maxStepSize * 2)).setSize(mapSize + 1);
        noMountains.blur(mapSize / 64);

        mountains.invert().minus(noMountains);
    }
}

