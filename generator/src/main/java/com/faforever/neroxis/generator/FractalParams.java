package com.faforever.neroxis.generator;

import java.util.List;

public record FractalParams(
        float waterHeight,
        FractalWaterMasks fractalWaterMask,
        int noiseMapBlurAmount,          // The blur radius to apply to the entire noise map, values of 1 - 4 are good here
        float noiseOctaveMultiplier,     // The influence of larger noise frequencies, 1.0f to 2.0f is a good range
        float noiseExpMultiplier,        // The overall slop of the noise map, 1 is no effect, 7 is very peaky mountains
        int teamSeparation,              // The fraction of the map that separates teams:
                                         //     2 means half the map will separate teams
                                         //     4 means that separated by 25% of the map will separate teams (front spawns can be closer)
        int spawnMaskDeflate,            // The distance from the edge of the spawnable layer, default is 4
        List<FractalFlattenParams> fractalFlattenParams
) {}
