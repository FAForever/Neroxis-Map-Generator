package com.faforever.neroxis.generator;

import java.util.List;

public record FractalParams(
        float waterHeight,
        FractalWaterMasks fractalWaterMask,
        int noiseMapBlurAmount,
        float noiseOctaveMultiplier,
        float noiseExpMultiplier,
        int teamSeparation,
        List<FractalFlattenParams> fractalFlattenParams
) {}
