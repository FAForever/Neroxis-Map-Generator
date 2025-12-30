package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;

import java.util.List;

public class FractalUpsideDownLastTerrainGenerator extends FractalNoiseLastTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        fractalParams = new FractalParams(
                2, false, 2, 1.2f, 3, 3,
                List.of(
                        new FractalFlattenParams(0f, 0.8f, 0, 10, 0.5f, 0, false, false, 4),
                        new FractalFlattenParams(0.8f, 15f, 16, 16, 0, 1, true, true, 4),
                        new FractalFlattenParams(15f, 50f, 12.5f, 12.5f, 0.5f, 1, false, false, 4)
                        )
        );

        super.initialize(map, seed, generatorParameters, symmetrySettings);
    }
}
