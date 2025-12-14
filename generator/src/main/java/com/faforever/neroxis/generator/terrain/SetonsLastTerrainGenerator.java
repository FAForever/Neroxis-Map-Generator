package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.FractalWaterMasks;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.Pipeline;

import java.util.List;

public class SetonsLastTerrainGenerator extends FractalNoiseLastTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        fractalParams = new FractalParams(
                16, FractalWaterMasks.SETONS, 2, 1.5f, 5, 2,
                List.of(
                        new FractalFlattenParams(0f, 1.0f, 0, 8, 1f, 0, false, false, 4),
                        new FractalFlattenParams(1.0f, 3f, 8, 16, 1f, 0, true, false, 4),
                        new FractalFlattenParams(3f, 27, 18, 18, 0, 1, false, true, 4),
                        new FractalFlattenParams(27, 50, 18, 35, 1, 1, false, false, 4)
                        )
        );
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
    }

    @Override
    protected void setupHeightmapPipeline() {
        // This extra step in the heightmap pipeline creates islands in the water area
        // It raises the underwater mountains to be above water
        landNoiseMap.multiply(waterAreaBlur.copy().add(1f).scaleExponentially(1.5f));
        super.setupHeightmapPipeline();
    }


}
