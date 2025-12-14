package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.util.SpawnPlacementException;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.SpawnPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import lombok.Getter;

@Getter
public abstract class SpawnLastTerrainGenerator extends TerrainGenerator {
    protected BooleanMask spawnMask;
    private BooleanMask spawnWaterMask;

    private SpawnPlacer spawnPlacer;

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
        pipeline.setDebug(true);
        spawnMask = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "spawnMask", pipeline);
        spawnWaterMask = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "spawnWaterMask",
                                         pipeline);
        spawnPlacer = new SpawnPlacer(map, random.nextLong());
    }

    protected void setupSpawnMaskPipeline() {
        spawnWaterMask.init(unbuildable.copy().invert().deflate(8));
        spawnMask.init(spawnWaterMask)
                 .multiply(heightmap.copyAsBooleanMask(map.getBiome().waterSettings().elevation()));
    }

    @Override
    public void placeSpawns() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeSpawns", () -> {
            if (spawnPlacer.placeSpawns(generatorParameters.spawnCount(), spawnMask.getFinalMask(), 48,
                                        getTeamSeparation())) {
                return;
            }

            throw new SpawnPlacementException("Unable to place all spawns");
        });
    }

    @Override
    public final void setupPipeline() {
        setupTerrainPipeline();
        //ensure heightmap is symmetric
        heightmap.forceSymmetry();
        setupPassablePipeline();
        setupSpawnMaskPipeline();
    }

    protected abstract void setupTerrainPipeline();
}
