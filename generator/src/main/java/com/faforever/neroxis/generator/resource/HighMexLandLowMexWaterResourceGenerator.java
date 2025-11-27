package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public class HighMexLandLowMexWaterResourceGenerator extends BasicResourceGenerator {

    private FloatMask waterResourceLimitNoiseMask;

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator, pipeline);

        waterResourceLimitNoiseMask = new FloatMask(1, random.nextLong(), symmetrySettings, "waterResourceLimitNoiseMask", pipeline);
        resourceDensity = random.nextFloat(1.5f, 2.0f);
    }

    @Override
    public void placeResources() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateResources", () -> {
            mexPlacer.placeMexes(getMexCount(), resourceMask.getFinalMask(), waterResourceMask.getFinalMask(), 16, 16, 12);
            hydroPlacer.placeHydros(generatorParameters.spawnCount(), resourceMask.getFinalMask().deflate(8));
        });
    }

    @Override
    protected int getMexCount() {
        int mapSize = generatorParameters.mapSize();
        int spawnCount = generatorParameters.spawnCount();

        // Add about 24 mexes per 256 chunk of the map multiplied by resource density
        int mexCount = StrictMath.round( (mapSize / 256f) * 24f * resourceDensity);

        // Add an additional 4 mexes per player
        mexCount += spawnCount * 4;

        return mexCount;
    }

    @Override
    public void setupPipeline() {
        waterResourceLimitNoiseMask.setSize(passableLand.getSize());
        waterResourceLimitNoiseMask.addWhiteNoise(0, 1);

        resourceMask.init(passableLand);
        resourceMask.add(passableWater
                                 .copy()
                                 .subtract(waterResourceLimitNoiseMask.copyAsBooleanMask(0.1f)));
        resourceMask.subtract(unbuildable.copy().inflate(2));

        waterResourceMask.setSize(resourceMask.getSize());
    }
}
