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
                .waterHeight(-5f)
                .fractalWaterMask(FractalWaterMasks.NONE)
                .noiseMapBlurAmount(1)
                .noiseSmallestDetail(2)
                .noiseOctaveMultiplier(1.5f)
                .noiseExpMultiplier(8)
                .teamSeparation(2)
                .spawnMaskDeflate(4)
                .clampMapHeight(22)
                .fractalFlattenParams(List.of(
                        FractalFlattenParams.builder()
                                .minHeight(0).maxHeight(1).destinationMinHeight(0).destinationMaxHeight(1)
                                .slope(0.01f).edgeBlur(0).hasRamps(true).rampPercentage(0.2f).spawnable(true).spawnMaskDeflate(4).build(),
                        FractalFlattenParams.builder()
                                .minHeight(1).maxHeight(4).destinationMinHeight(1).destinationMaxHeight(1)
                                .slope(0).edgeBlur(0).hasRamps(true).rampPercentage(0.2f).spawnable(false).spawnMaskDeflate(4).build(),
                        FractalFlattenParams.builder()
                                .minHeight(4).maxHeight(6).destinationMinHeight(13).destinationMaxHeight(13)
                                .slope(0).edgeBlur(2).hasRamps(false).rampPercentage(0f).spawnable(false).spawnMaskDeflate(4).build(),
                        FractalFlattenParams.builder()
                                .minHeight(6).maxHeight(15).destinationMinHeight(11).destinationMaxHeight(11)
                                .slope(0).edgeBlur(0).hasRamps(false).rampPercentage(0f).spawnable(false).spawnMaskDeflate(4).build(),
                        FractalFlattenParams.builder()
                                .minHeight(15).maxHeight(22).destinationMinHeight(16).destinationMaxHeight(16)
                                .slope(0).edgeBlur(1).hasRamps(false).rampPercentage(0f).spawnable(false).spawnMaskDeflate(4).build()
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
