package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.SpawnPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import lombok.Getter;

@Getter
public abstract class SpawnLastTerrainGenerator extends TerrainGenerator {
    private BooleanMask spawnMask;

    private SpawnPlacer spawnPlacer;

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
        spawnMask = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "spawnMask",
                                    pipeline);
        spawnPlacer = new SpawnPlacer(map, random.nextLong());
    }

    private void setupSpawnMaskPipeline() {
        spawnMask.init(unbuildable.copy().invert().deflate(8));
    }

    @Override
    public void placeSpawns() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeSpawns",
                           () -> spawnPlacer.placeSpawns(generatorParameters.spawnCount(), spawnMask.getFinalMask(),
                                                         getSpawnSeparation(), getTeamSeparation()));
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
