package com.faforever.neroxis.generator.terrain;

import lombok.Getter;

@Getter
public abstract class SpawnFirstTerrainGenerator extends TerrainGenerator {

    @Override
    public void placeSpawns() {}

    @Override
    public final void setupPipeline() {
        setupTerrainPipeline();
        if (generatorParameters.symmetrySettings().terrainSymmetry().isPerfectSymmetry()) {
            heightmap.forceSymmetry();
        } else {
            heightmap.forceSmoothTerrainRotation(generatorParameters.numTeams(),
                                                 map.getBiome().waterSettings().elevation(),
                                                 getOuterCircleTerrain());
        }
        setupPassablePipeline();
    }

    protected abstract void setupTerrainPipeline();
}
