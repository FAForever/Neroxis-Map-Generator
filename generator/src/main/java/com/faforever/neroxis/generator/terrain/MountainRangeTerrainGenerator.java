package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;

import java.util.random.RandomGenerator;

public class MountainRangeTerrainGenerator extends PathedPlateauTerrainGenerator {


    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, random, generatorParameters, symmetrySettings);
        mountainBrushSize = map.getSize() / 16;
        mountainBrushDensity = 1.25f;
        mountainBrushIntensity = 3f;
    }

    @Override
    protected void mountainSetup() {
        int mapSize = map.getSize();
        mountains.setSize(mapSize / 2);

        mountains.progressiveWalk(
                (int) (mountainDensity * 16 / symmetrySettings.terrainSymmetry().getNumSymPoints()) + 8,
                mapSize / 4);
        mountains.inflate(2);

        mountains.setSize(mapSize + 1);
    }
}
