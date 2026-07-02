package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.OnePerBaseHydroPlacer;
import com.faforever.neroxis.util.DebugUtil;

public class OneHydroPerSpawnResourceGenerator extends BasicResourceGenerator {

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                                  SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        hydroPlacer = new OnePerBaseHydroPlacer(map, random.nextLong());
    }

    @Override
    public void placeResources() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateResources", () -> {
            hydroPlacer.placeHydros(generatorParameters.spawnCount(), resourceMask.getFinalMask().deflate(8));
        });
    }
}
