package com.faforever.neroxis.map.generator.terrain;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.MapMaskMethods;

public strictfp class SmallIslandsTerrainGenerator extends PathedTerrainGenerator {

    public SmallIslandsTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .5f)
                .plateauDensity(0, .5f)
                .mexDensity(.5f, 1)
                .mapSizes(768, 1024)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        spawnSize = 64;
    }

    @Override
    protected void landSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedLandDensity = parameterConstraints.getLandDensityRange().normalize(mapParameters.getLandDensity());
        int maxMiddlePoints = 4;
        int numPaths = (int) (4 * normalizedLandDensity + 4) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = ((int) (mapSize / 16 * (random.nextFloat() * .25f + normalizedLandDensity * .75f)) + mapSize / 16);
        float maxStepSize = mapSize / 128f;

        BooleanMask islands = new BooleanMask(mapSize / 4, random.nextLong(), symmetrySettings, "islands", true);

        land.setSize(mapSize + 1);
        MapMaskMethods.pathAroundSpawns(map, random.nextLong(), land, maxStepSize, numPaths, maxMiddlePoints, bound, (float) StrictMath.PI / 2);
        land.inflate(maxStepSize).setSize(mapSize / 4);

        islands.randomWalk((int) (normalizedLandDensity * 6 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()) + 8, mapSize / 8);

        land.add(islands);
        land.dilute(.5f, 8);

        land.setSize(mapSize + 1);
        land.blur(16);
    }
}


