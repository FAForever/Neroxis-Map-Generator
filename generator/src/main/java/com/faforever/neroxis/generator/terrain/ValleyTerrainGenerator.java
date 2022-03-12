package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.MapMaskMethods;

public strictfp class ValleyTerrainGenerator extends PathedPlateauTerrainGenerator {

    public ValleyTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.75f, 1f)
                .mountainDensity(.5f, 1)
                .mapSizes(384, 1024)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters) {
        super.initialize(map, seed, generatorParameters);
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
        SymmetrySettings symmetrySettings = generatorParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedMountainDensity = parameterConstraints.getMountainDensityRange().normalize(generatorParameters.getMountainDensity());
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 8;
        int numPaths = (int) (4 + 4 * (1 - normalizedMountainDensity) / symmetrySettings.getTerrainSymmetry().getNumSymPoints());
        int bound = (int) (mapSize / 16 * (2 * (random.nextFloat() * .25f + normalizedMountainDensity * .75f) + 2));
        mountains.setSize(mapSize + 1);
        BooleanMask noMountains = new BooleanMask(mapSize + 1, random.nextLong(), symmetrySettings, "noMountains", true);

        MapMaskMethods.pathInCenterBounds(random.nextLong(), noMountains, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        noMountains.setSize(mapSize / 4);
        noMountains.dilute(.5f, (int) (maxStepSize * 2)).setSize(mapSize + 1);
        noMountains.blur(mapSize / 64).inflate(mountainBrushSize / 16f);

        mountains.invert().subtract(noMountains);
    }
}

