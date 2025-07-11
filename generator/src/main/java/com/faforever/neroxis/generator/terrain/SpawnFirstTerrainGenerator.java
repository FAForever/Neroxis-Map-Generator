package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.SpawnPlacer;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import lombok.Getter;

@Getter
public abstract class SpawnFirstTerrainGenerator extends TerrainGenerator {
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
        SpawnPlacer spawnPlacer = new SpawnPlacer(map, random.nextLong());
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeSpawns",
                           () -> spawnPlacer.placeSpawns(generatorParameters.spawnCount(), getSpawnSeparation(),
                                                         getTeamSeparation(), symmetrySettings));
    }

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
