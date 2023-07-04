package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public abstract class SpawnLastTerrainGenerator extends TerrainGenerator {

    protected BooleanMask spawnLand;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters, SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        spawnLand = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "spawnLand", true);
    }

    @Override
    protected void passableSetup() {
        super.passableSetup();
        spawnLand.add(passableLand.copy().deflate(8f));
    }

    @Override
    public void setHeightmap() {
        super.setHeightmap();

        Pipeline.await(spawnLand);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeSpawns", () -> spawnPlacer.placeSpawns(generatorParameters.spawnCount(), spawnLand.getFinalMask(), spawnSeparation, teamSeparation));
    }

}
