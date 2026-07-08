package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.util.serial.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.MapMaskMethods;

import java.util.random.RandomGenerator;

public class CenterLakeTerrainGenerator extends PathedTerrainGenerator {

    private BooleanMask noLand;

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mapSizes(384, 1024)
                                   .build();
    }

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, random, generatorParameters, symmetrySettings);
        noLand = new BooleanMask(map.getSize() + 1, random.split(), symmetrySettings, "noLand");
        mountainBrushSize = 32;
        mountainBrushDensity = .05f;
        mountainBrushIntensity = 10;
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 8;
        int numWalkers = (int) (8 * (1 - landDensity) + 8) / symmetrySettings.spawnSymmetry().getNumSymPoints();
        int bound = (int) (mapSize / 64f * (24 * (random.nextFloat() * .25f + landDensity * .75f)))
                    + mapSize / 8;
        land.setSize(mapSize + 1);
        land.invert();

        MapMaskMethods.pathInCenterBounds(random.split(), noLand, maxStepSize, numWalkers, maxMiddlePoints, bound,
                                          (float) (StrictMath.PI / 2));
        noLand.inflate(1).setSize(mapSize / 4);
        noLand.dilute(.5f, 10).setSize(mapSize + 1);
        noLand.blur(mapSize / 64, .5f);
        land.subtract(noLand);
    }
}


