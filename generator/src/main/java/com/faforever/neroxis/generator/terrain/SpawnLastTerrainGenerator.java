package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.util.SpawnPlacementException;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.SpawnPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import lombok.Getter;

@Getter
public abstract class SpawnLastTerrainGenerator extends TerrainGenerator {
    protected BooleanMask spawnMask;
    private BooleanMask spawnWaterMask;

    private SpawnPlacer spawnPlacer;

    protected int getMinTeammateSeparation() {
        return map.getSize() / 8;
    }

    protected int getMaxTeammateSeparation() {
        return map.getSize() / 4;
    }

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        spawnMask = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "spawnMask");
        spawnWaterMask = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "spawnWaterMask");
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
            if (spawnPlacer.placeSpawns(generatorParameters.spawnCount(), spawnMask.getFinalMask(),
                                        getMinTeammateSeparation(), getMaxTeammateSeparation(),
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
