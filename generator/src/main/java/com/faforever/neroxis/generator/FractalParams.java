package com.faforever.neroxis.generator;

public record FractalParams(
        float waterHeight,
        boolean useRandomWaterMask,
        int noiseMapBlurAmount,
        float noiseOctaveMultiplier,
        float noiseExpMultiplier,
        int teamSeparation,
        FractalFlattenParams[] fractalFlattenParams
) {}
