package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.util.serial.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;

import java.util.random.RandomGenerator;

public class LittleMountainTerrainGenerator extends PathedPlateauTerrainGenerator {

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
                (int) (mountainDensity * 250 / symmetrySettings.terrainSymmetry().getNumSymPoints() + 100),
                mapSize / 128);

        mountains.setSize(mapSize + 1);
    }
}
