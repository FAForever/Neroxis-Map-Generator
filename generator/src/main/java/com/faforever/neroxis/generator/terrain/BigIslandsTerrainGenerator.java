package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.MapMaskMethods;

public class BigIslandsTerrainGenerator extends PathedTerrainGenerator {
    public BigIslandsTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                                                   .landDensity(0f, .75f)
                                                   .plateauDensity(0, .5f)
                                                   .mapSizes(768, 1024)
                                                   .build();
        weight = 2;
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();
        float normalizedLandDensity = parameterConstraints.getLandDensityRange()
                                                          .normalize(generatorParameters.getLandDensity());
        int maxMiddlePoints = 4;
        int numPaths = (int) (8 * normalizedLandDensity + 8) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = ((int) (mapSize / 8 * (random.nextFloat() * .25f + normalizedLandDensity * .75f)) + mapSize / 8);
        float maxStepSize = mapSize / 128f;

        BooleanMask islands = new BooleanMask(mapSize / 4, random.nextLong(), symmetrySettings, "islands", true);

        land.setSize(mapSize + 1);
        MapMaskMethods.pathAroundSpawns(map, random.nextLong(), land, maxStepSize, numPaths, maxMiddlePoints, bound,
                                        (float) StrictMath.PI / 2);
        land.inflate(maxStepSize).setSize(mapSize / 4);

        islands.randomWalk(
                (int) (normalizedLandDensity * 20 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()) + 2,
                mapSize * 4);
        islands.subtract(land.copy().inflate(32));

        land.add(islands);
        land.dilute(.5f, 8);

        land.setSize(mapSize + 1);
        land.blur(16);
    }
}


