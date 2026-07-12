package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;

import java.util.random.RandomGenerator;

public class LittleMountainLastTerrainGenerator extends PathedPlateauLastTerrainGenerator {

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, random, generatorParameters, symmetrySettings);
        mountainBrushSize = 24;
        mountainBrushDensity = .35f;
        mountainBrushIntensity = 8;
    }

    @Override
    protected void mountainSetup() {
        int mapSize = map.getSize();
        mountains.setSize(mapSize / 4);

        mountains.randomWalk(
                (int) (mountainDensity * 125 / symmetrySettings.terrainSymmetry().getNumSymPoints() + 50),
                mapSize / 128);

        mountains.setSize(mapSize + 1);
    }
}
