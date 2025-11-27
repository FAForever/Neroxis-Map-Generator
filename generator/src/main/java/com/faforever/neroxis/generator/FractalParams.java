package com.faforever.neroxis.generator;

import java.util.List;

public record FractalParams(
        float waterHeight,
        boolean useRandomWaterMask,
        int noiseMapBlurAmount,
        float noiseOctaveMultiplier,
        float noiseExpMultiplier,
        int teamSeparation,
        List<FractalFlattenParams> fractalFlattenParams
) {}
