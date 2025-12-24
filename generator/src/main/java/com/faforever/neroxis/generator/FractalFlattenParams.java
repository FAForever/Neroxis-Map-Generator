package com.faforever.neroxis.generator;

public record FractalFlattenParams(
    float minHeight,             // The minimum height from the source data to flatten
    float maxHeight,             // The maximum height from the source data to flatten
    float destinationMinHeight,  // The minimum height to use in the destination map
    float destinationMaxHeight,  // The maximum height to use in the destination map (If min and max are the same it will be flat)
    float slope,                 // The slope to use when destinationMaxHeight > destinationMinHeight, > 1 for an exponential slope
    int blurAmount,              // Blur to apply over the layer (0 is no blur)
    boolean hasRamps,            // True if there should be pathable Ramps to the lower layer
    boolean spawnable,           // True if spawn point can generate on this layer
    float spawnMaskDeflate       // Distance from the edge of the layer, that a play can't spawn on.
) {}
