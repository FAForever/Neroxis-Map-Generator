package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public strictfp class BasicResourceGenerator extends ResourceGenerator {
    protected BooleanMask resourceMask;
    protected BooleanMask waterResourceMask;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters, SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        resourceMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "resourceMask", true);
        waterResourceMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "waterResourceMask", true);
    }

    @Override
    public void setupPipeline() {
        resourceMask.init(passableLand);
        waterResourceMask.init(passableLand).invert();

        resourceMask.subtract(unbuildable).deflate(4);
        resourceMask.fillEdge(16, false).fillCenter(24, false);
        waterResourceMask.subtract(unbuildable).deflate(8).fillEdge(16, false).fillCenter(24, false);
    }

    @Override
    public void placeResources() {
        Pipeline.await(resourceMask, waterResourceMask);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateResources", () -> {
            mexPlacer.placeMexes(getMexCount(), resourceMask.getFinalMask(), waterResourceMask.getFinalMask());
            hydroPlacer.placeHydros(generatorParameters.getSpawnCount(), resourceMask.getFinalMask().deflate(8));
        });
    }

    @Override
    protected int getMexCount() {
        int mexCount;
        int mapSize = generatorParameters.getMapSize();
        int spawnCount = generatorParameters.getSpawnCount();
        float mexDensity = generatorParameters.getMexDensity();
        float mexMultiplier = 1f;
        if (spawnCount <= 2) {
            mexCount = (int) (10 + 20 * mexDensity);
        } else if (spawnCount <= 4) {
            mexCount = (int) (12 + 6 * mexDensity);
        } else if (spawnCount <= 10) {
            mexCount = (int) (8 + 7 * mexDensity);
        } else if (spawnCount <= 12) {
            mexCount = (int) (6 + 7 * mexDensity);
        } else {
            mexCount = (int) (6 + 7 * mexDensity);
        }
        if (mapSize < 384) {
            mexMultiplier = .5f;
        } else if (mapSize >= 768) {
            if (spawnCount <= 4) {
                mexMultiplier = 1.75f;
            } else if (spawnCount <= 6) {
                mexMultiplier = 1.5f;
            } else if (spawnCount <= 10) {
                mexMultiplier = 1.35f;
            } else {
                mexMultiplier = 1.25f;
            }
        }
        mexCount *= mexMultiplier;
        mexCount = StrictMath.max(mexCount, 9);
        return mexCount * spawnCount;
    }
}
