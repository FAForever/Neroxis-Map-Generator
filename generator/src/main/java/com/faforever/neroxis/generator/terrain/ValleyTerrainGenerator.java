package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.MapMaskMethods;

import java.util.random.RandomGenerator;

public class ValleyTerrainGenerator extends PathedPlateauTerrainGenerator {

    private BooleanMask noMountains;

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, random, generatorParameters, symmetrySettings);
        noMountains = new BooleanMask(map.getSize() + 1, random.split(), symmetrySettings, "noMountains");
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
        int mapSize = map.getSize();
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 8;
        int numPaths = (int) (4 + 4 * (1 - mountainDensity) / symmetrySettings.terrainSymmetry().getNumSymPoints());
        int bound = (int) (mapSize / 16f * (2 * (random.nextFloat() * .25f + mountainDensity * .75f) + 2));
        mountains.setSize(mapSize + 1);

        MapMaskMethods.pathInCenterBounds(random.split(), noMountains, maxStepSize, numPaths, maxMiddlePoints, bound,
                                          (float) (StrictMath.PI / 2));
        noMountains.setSize(mapSize / 4);
        noMountains.dilute(.5f, (int) (maxStepSize * 2)).setSize(mapSize + 1);
        noMountains.blur(mapSize / 64).inflate(mountainBrushSize / 16);

        mountains.invert().subtract(noMountains);
    }
}

