package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;

public class BigIslandsSpawnLastTerrainGenerator extends PathedSpawnLastTerrainGenerator {
    public BigIslandsSpawnLastTerrainGenerator() {
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
                                                          .normalize(generatorParameters.landDensity());

        float threshold = .6f + normalizedLandDensity * .15f;

        FloatMask islandPerlin = new FloatMask(mapSize +
                                               1, random.nextLong(), land.getSymmetrySettings(), "islandPerlin", true);

        islandPerlin.addPerlinNoise(mapSize / 4, 1)
                    .addPerlinNoise(mapSize / 8, .5f)
                    .addPerlinNoise(mapSize / 32, .25f);

        BooleanMask islands = islandPerlin.copyAsBooleanMask(-threshold, threshold)
                                          .resample(mapSize / 8)
                                          .dilute(.5f, 2)
                                          .resample(mapSize + 1)
                                          .blur(8);

        land.setSize(mapSize + 1);
        land.add(islands);
    }

    @Override
    protected void plateausSetup() {
        plateaus.setSize(map.getSize() + 1);
    }

    @Override
    protected void mountainSetup() {
        mountains.setSize(map.getSize() + 1);
    }
}


