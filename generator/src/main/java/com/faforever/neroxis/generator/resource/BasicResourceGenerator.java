package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public class BasicResourceGenerator extends ResourceGenerator {
    @Override
    public void placeResources() {
        Pipeline.await(resourceMask, waterResourceMask);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateResources", () -> {
            mexPlacer.placeMexes(getMexCount(), resourceMask.getFinalMask(), waterResourceMask.getFinalMask());
            hydroPlacer.placeHydros(generatorParameters.spawnCount(), resourceMask.getFinalMask().deflate(8));
        });
    }

    @Override
    protected int getMexCount() {
        int mexCount;
        int mapSize = generatorParameters.mapSize();
        int spawnCount = generatorParameters.spawnCount();
        float mexMultiplier = 1f;
        if (spawnCount <= 2) {
            mexCount = (int) (10 + 20 * resourceDensity);
        } else if (spawnCount <= 4) {
            mexCount = (int) (12 + 6 * resourceDensity);
        } else if (spawnCount <= 10) {
            mexCount = (int) (8 + 7 * resourceDensity);
        } else if (spawnCount <= 12) {
            mexCount = (int) (6 + 7 * resourceDensity);
        } else {
            mexCount = (int) (6 + 7 * resourceDensity);
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
        mexCount = StrictMath.round(mexCount * mexMultiplier);
        mexCount = StrictMath.max(mexCount, 9);
        return mexCount * spawnCount;
    }

    @Override
    protected void setupPipeline() {
        resourceMask.init(passableLand);
        waterResourceMask.init(passableLand).invert();

        resourceMask.subtract(unbuildable).deflate(4);
        resourceMask.fillEdge(16, false).fillCenter(24, false, SymmetryType.TEAM);
        waterResourceMask.subtract(unbuildable).deflate(8).fillEdge(16, false).fillCenter(24, false, SymmetryType.TEAM);
    }
}
