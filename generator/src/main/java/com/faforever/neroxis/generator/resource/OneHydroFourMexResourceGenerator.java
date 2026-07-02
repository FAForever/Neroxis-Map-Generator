package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.FourPerBaseMexPlacer;
import com.faforever.neroxis.map.placement.OnePerBaseHydroPlacer;

public class OneHydroFourMexResourceGenerator extends BasicResourceGenerator
{

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        hydroPlacer = new OnePerBaseHydroPlacer(map, random.nextLong());
        mexPlacer = new FourPerBaseMexPlacer(map, random.nextLong());
    }

}
