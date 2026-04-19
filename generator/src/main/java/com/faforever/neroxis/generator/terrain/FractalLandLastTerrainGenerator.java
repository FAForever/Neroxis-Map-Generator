package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.FractalWaterMasks;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;

import java.util.List;

public class FractalLandLastTerrainGenerator extends FractalNoiseLastTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        fractalParams = FractalParams.builder()
                .waterHeight(-5.0f)
                .fractalWaterMask(FractalWaterMasks.NONE)
                .noiseMapBlurAmount(1)
                .noiseSmallestDetail(2)
                .noiseOctaveMultiplier(1.5f)
                .noiseExpMultiplier(8.0f)
                .teamSeparation(2)
                .spawnMaskDeflate(4)
                .clampMapHeight(22.0f)
                .fractalFlattenParams(List.of(
                        FractalFlattenParams.builder()
                                            .minHeight(0.0f).maxHeight(1.0f)
                                            .destinationMinHeight(0.0f).destinationMaxHeight(1.0f)
                                            .slope(0.01f).edgeBlur(0).hasRamps(true).rampPercentage(0.2f)
                                            .spawnable(true).spawnMaskDeflate(4).build(),
                        FractalFlattenParams.builder()
                                            .minHeight(1.0f).maxHeight(4.0f)
                                            .destinationMinHeight(1.0f).destinationMaxHeight(1.0f)
                                            .slope(0.0f).edgeBlur(0).hasRamps(true).rampPercentage(0.2f)
                                            .spawnable(false).spawnMaskDeflate(4).build(),
                        FractalFlattenParams.builder()
                                            .minHeight(4.0f).maxHeight(6.0f)
                                            .destinationMinHeight(13.0f).destinationMaxHeight(13.0f)
                                            .slope(0.0f).edgeBlur(2).hasRamps(false).rampPercentage(0.0f)
                                            .spawnable(false).spawnMaskDeflate(4).build(),
                        FractalFlattenParams.builder()
                                            .minHeight(6.0f).maxHeight(15.0f)
                                            .destinationMinHeight(11.0f).destinationMaxHeight(11.0f)
                                            .slope(0.0f).edgeBlur(0).hasRamps(false).rampPercentage(0.0f)
                                            .spawnable(false).spawnMaskDeflate(4).build(),
                        FractalFlattenParams.builder()
                                            .minHeight(15.0f).maxHeight(22.0f)
                                            .destinationMinHeight(16.0f).destinationMaxHeight(16.0f)
                                            .slope(0.0f).edgeBlur(1).hasRamps(false).rampPercentage(0.0f)
                                            .spawnable(false).spawnMaskDeflate(4).build()
                ))
                .build();

        super.initialize(map, seed, generatorParameters, symmetrySettings);
    }

    @Override
    protected void setMaxNoiseOctaves() {
        if (map.getSize() > 768) {
            maxNoiseOctaves = 8;
        } else {
            maxNoiseOctaves = 7;
        }
    }
}
