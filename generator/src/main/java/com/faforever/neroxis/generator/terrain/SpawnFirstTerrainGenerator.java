package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.util.DebugUtil;

public abstract class SpawnFirstTerrainGenerator extends TerrainGenerator {

    @Override
    protected void terrainSetup() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeSpawns", () -> spawnPlacer.placeSpawns(generatorParameters.spawnCount(), spawnSeparation, teamSeparation, symmetrySettings));
    }

}
