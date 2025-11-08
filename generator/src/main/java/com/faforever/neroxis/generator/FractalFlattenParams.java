package com.faforever.neroxis.generator;

public record FractalFlattenParams(
    float minHeight,
    float maxHeight,
    float destinationMinHeight,
    float destinationMaxHeight,
    float slope,
    int blurAmount,
    boolean hasRamps,
    boolean spawnable,
    float spawnMaskDeflate
) {}
