package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.FourPerBaseMexPlacer;
import com.faforever.neroxis.map.placement.OnePerBaseHydroPlacer;

import java.util.random.RandomGenerator;

public class OneHydroFourMexResourceGenerator extends BasicResourceGenerator {

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, random, generatorParameters, symmetrySettings, terrainGenerator);
        hydroPlacer = new OnePerBaseHydroPlacer(map, random.split());
        mexPlacer = new FourPerBaseMexPlacer(map, random.split());
    }

}
