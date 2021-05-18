package com.faforever.neroxis.map.generator.resource;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.mask.BooleanMask;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.Util;

public strictfp class BasicResourceGenerator extends ResourceGenerator {
    protected BooleanMask resourceMask;
    protected BooleanMask waterResourceMask;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
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
        Util.timedRun("com.faforever.neroxis.map.generator", "generateResources", () -> {
            mexPlacer.placeMexes(getMexCount(), resourceMask.getFinalMask(), waterResourceMask.getFinalMask());
            hydroPlacer.placeHydros(mapParameters.getHydroCount(), resourceMask.getFinalMask().deflate(8));
        });
    }

    @Override
    protected int getMexCount() {
        int mexCount;
        int mapSize = mapParameters.getMapSize();
        int spawnCount = mapParameters.getSpawnCount();
        float mexDensity = mapParameters.getMexDensity();
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
        if (mapSize < 512) {
            mexMultiplier = .5f;
        } else if (mapSize > 512) {
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
        return mexCount * spawnCount;
    }
}
