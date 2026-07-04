package com.faforever.neroxis.generator.terrain;

import lombok.Getter;

@Getter
public abstract class SpawnFirstTerrainGenerator extends TerrainGenerator {

    @Override
    public void placeSpawns() {}

    @Override
    public final void setupPipeline() {
        setupTerrainPipeline();
        //ensure heightmap is symmetric
        heightmap.forceSymmetry();
        setupPassablePipeline();
    }

    protected abstract void setupTerrainPipeline();
}
