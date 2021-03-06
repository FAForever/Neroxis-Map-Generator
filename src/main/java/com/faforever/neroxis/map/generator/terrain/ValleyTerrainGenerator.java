package com.faforever.neroxis.map.generator.terrain;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
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
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        mountainBrushSize = 48;
        mountainBrushDensity = .25f;
        mountainBrushIntensity = 4f;
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
        int numPaths = (int) (4 + 4 * (1 - normalizedMountainDensity) / symmetrySettings.getTerrainSymmetry().getNumSymPoints());
        int bound = (int) (mapSize / 16 * (2 * (random.nextFloat() * .25f + normalizedMountainDensity * .75f) + 2));
        mountains.setSize(mapSize + 1);
        BooleanMask noMountains = new BooleanMask(mapSize + 1, random.nextLong(), symmetrySettings, "noMountains", true);

        pathInCenterBounds(noMountains, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        noMountains.setSize(mapSize / 4);
        noMountains.dilute(.5f, (int) (maxStepSize * 2)).setSize(mapSize + 1);
        noMountains.blur(mapSize / 64).inflate(mountainBrushSize / 16f);

        mountains.invert().subtract(noMountains);
    }
}

