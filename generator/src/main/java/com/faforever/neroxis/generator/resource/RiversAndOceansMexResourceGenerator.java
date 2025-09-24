package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.Pipeline;

public class RiversAndOceansMexResourceGenerator extends BasicResourceGenerator {

    private FloatMask waterResourceLimitNoiseMask;

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator, pipeline);

        waterResourceLimitNoiseMask = new FloatMask(1, random.nextLong(), symmetrySettings, "waterResourceLimitNoiseMask", pipeline);
    }

    @Override
    public void setupPipeline() {
        waterResourceLimitNoiseMask.setSize(passableLand.getSize());
        waterResourceLimitNoiseMask.addWhiteNoise(0, 1);

        resourceMask.init(passableLand);
        resourceMask.add(passableWater
                                 .copy()
                                 .subtract(waterResourceLimitNoiseMask.copyAsBooleanMask(0.01f)));
        resourceMask.subtract(unbuildable.copy().inflate(2));

        waterResourceMask.setSize(resourceMask.getSize());
    }
}
