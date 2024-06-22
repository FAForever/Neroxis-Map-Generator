package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.SpawnPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import lombok.Getter;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public abstract class TerrainGenerator implements HasParameterConstraints {
    protected SCMap map;
    protected Random random;
    protected GeneratorParameters generatorParameters;
    protected SymmetrySettings symmetrySettings;

    protected SpawnPlacer spawnPlacer;

    @Getter
    protected FloatMask heightmap;
    @Getter
    protected BooleanMask impassable;
    @Getter
    protected BooleanMask unbuildable;
    @Getter
    protected BooleanMask passable;
    @Getter
    protected BooleanMask passableLand;
    @Getter
    protected BooleanMask passableWater;
    @Getter
    protected FloatMask slope;
    protected CompletableFuture<Void> spawnsSetFuture;
    private CompletableFuture<Void> heightmapSetFuture;

    private void setHeightmapImage() {
        Pipeline.await(heightmap);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "setHeightMap", () -> heightmap.getFinalMask()
                                                                                                 .writeToImage(
                                                                                                         map.getHeightmap(),
                                                                                                         1
                                                                                                         /
                                                                                                         map.getHeightMapScale()));
    }

    protected abstract void placeSpawns();

    public final CompletableFuture<Void> getSpawnsSetFuture() {
        return spawnsSetFuture.copy();
    }

    public final CompletableFuture<Void> getHeightmapSetFuture() {
        return heightmapSetFuture.copy();
    }

    private void setupPipeline() {
        setupTerrainPipeline();
        //ensure heightmap is symmetric
        heightmap.forceSymmetry();
        setupPassablePipeline();
    }

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        this.map = map;
        this.random = new Random(seed);
        this.generatorParameters = generatorParameters;
        this.symmetrySettings = symmetrySettings;
        heightmap = new FloatMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "heightmap", true);
        slope = new FloatMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "slope", true);
        impassable = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "impassable", true);
        unbuildable = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "unbuildable", true);
        passable = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passable", true);
        passableLand = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passableLand", true);
        passableWater = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passableWater", true);

        spawnPlacer = new SpawnPlacer(map, random.nextLong());

        afterInitialize();

        heightmapSetFuture = CompletableFuture.runAsync(this::setHeightmapImage);
        spawnsSetFuture = CompletableFuture.runAsync(this::placeSpawns);

        setupPipeline();
    }

    protected void afterInitialize() {}

    protected abstract void setupTerrainPipeline();

    private void setupPassablePipeline() {
        BooleanMask actualLand = heightmap.copyAsBooleanMask(
                map.getBiome().waterSettings().elevation());

        slope.init(heightmap.copy().supcomGradient());
        impassable.init(slope, .7f);
        unbuildable.init(slope, .05f);

        impassable.inflate(4);

        passable.init(impassable).invert();
        passableLand.init(actualLand);
        passableWater.init(actualLand).invert();

        passable.fillEdge(8, false);
        passableLand.multiply(passable);
        passableWater.deflate(16).fillEdge(8, false);
    }

    protected float getSpawnSeparation() {
        if (generatorParameters.numTeams() < 2) {
            return (float) generatorParameters.mapSize() / generatorParameters.spawnCount() * 1.5f;
        } else if (generatorParameters.numTeams() == 2) {
            return random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16f;
        } else {
            if (generatorParameters.numTeams() < 8) {
                return random.nextInt(map.getSize() / 2 / generatorParameters.numTeams() - map.getSize() / 16) +
                       map.getSize() / 16f;
            } else {
                return 0;
            }
        }
    }

    protected int getTeamSeparation() {
        if (generatorParameters.numTeams() < 2) {
            return 0;
        } else if (generatorParameters.numTeams() == 2) {
            return map.getSize() / 2;
        } else {
            return StrictMath.min(map.getSize() / generatorParameters.numTeams(), 256);
        }
    }
}
