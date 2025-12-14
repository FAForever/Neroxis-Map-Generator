package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.FractalWaterMasks;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.Pipeline;

import java.util.List;

public class FractalPlateauLastTerrainGenerator extends FractalNoiseLastTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        fractalParams = new FractalParams(
                3f, FractalWaterMasks.NONE, 4, 1.2f, 4, 2,
                List.of(
                        new FractalFlattenParams(0f, 0.1f, 0, 4, 0.5f, 0, false, false, 4),
                        new FractalFlattenParams(0.1f, 1.0f, 4, 14, 2, 0, true, false, 4),
                        new FractalFlattenParams(1.0f, 27, 14, 15, 1f, 0, false, true, 4),
                        new FractalFlattenParams(27, 50, 24, 24, 0, 2, false, false, 4)
                )
        );

        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
    }
}
