package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.FractalWaterMasks;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;

import java.util.List;

public class FractalPlateauLastTerrainGenerator extends FractalNoiseLastTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        fractalParams = FractalParams.builder()
                .waterHeight(3.0f)
                .fractalWaterMask(FractalWaterMasks.NONE)
                .noiseMapBlurAmount(4)
                .noiseSmallestDetail(2)
                .noiseOctaveMultiplier(1.2f)
                .noiseExpMultiplier(4.0f)
                .teamSeparation(2)
                .spawnMaskDeflate(4)
                .clampMapHeight(50.0f)
                .fractalFlattenParams(List.of(
                        FractalFlattenParams.builder()
                                .minHeight(0.0f).maxHeight(0.1f).destinationMinHeight(0.0f).destinationMaxHeight(4.0f)
                                .slope(0.5f).edgeBlur(0).hasRamps(false).rampPercentage(0.0f).spawnable(false).spawnMaskDeflate(4).build(),
                        FractalFlattenParams.builder()
                                .minHeight(0.1f).maxHeight(1.0f).destinationMinHeight(4.0f).destinationMaxHeight(14.0f)
                                .slope(2.0f).edgeBlur(0).hasRamps(true).rampPercentage(0.0f).spawnable(false).spawnMaskDeflate(4).build(),
                        FractalFlattenParams.builder()
                                .minHeight(1.0f).maxHeight(27.0f).destinationMinHeight(14.0f).destinationMaxHeight(15.0f)
                                .slope(1.0f).edgeBlur(0).hasRamps(false).rampPercentage(0.1f).spawnable(true).spawnMaskDeflate(4).build(),
                        FractalFlattenParams.builder()
                                .minHeight(27.0f).maxHeight(50.0f).destinationMinHeight(24.0f).destinationMaxHeight(24.0f)
                                .slope(0.0f).edgeBlur(2).hasRamps(false).rampPercentage(0.0f).spawnable(false).spawnMaskDeflate(4).build()
                ))
                .build();

        super.initialize(map, seed, generatorParameters, symmetrySettings);
    }
}
