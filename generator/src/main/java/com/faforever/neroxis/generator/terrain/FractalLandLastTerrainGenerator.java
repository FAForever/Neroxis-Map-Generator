package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.Pipeline;

public class FractalLandLastTerrainGenerator extends FractalNoiseLastTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        fractalParams = new FractalParams(
                0f, false, 1, 1.5f, 8, 2,
                new FractalFlattenParams[]{
                        new FractalFlattenParams(0, 1, 0, 1, 0.5f, 0, true, true, 4),
                        new FractalFlattenParams(1, 4, 1, 1, 0, 0, false, false, 4),
                        new FractalFlattenParams(4, 6, 13, 13, 0, 2, false, false, 4),
                        new FractalFlattenParams(6, 15, 11, 11, 0, 0, false, false, 4),
                        new FractalFlattenParams(15, 22, 16, 16, 0, 1, false, false, 4),
                });

        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
    }
}
