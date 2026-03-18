package com.faforever.neroxis.generator;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;

/**
 * Parameters defining the generation and shaping of a fractal-based heightmap.
 *
 * @param waterHeight
 *        The height at which water is placed in the map.
 * @param fractalWaterMask
 *        The water mask configuration used to control where water can appear.
 * @param noiseMapBlurAmount
 *        The blur radius applied to the entire noise map.
 *        Values in the range {@code 1}–{@code 4} generally produce good results.
 * @param noiseOctaveMultiplier
 *        Controls the influence of larger noise frequencies.
 *        Typical values range from {@code 1.0f} to {@code 2.0f}.
 * @param noiseExpMultiplier
 *        Controls the overall slope of the noise map.
 *        A value of {@code 1.0f} has no effect, while higher values (e.g. {@code 7.0f})
 *        produce sharper, peakier mountains.
 * @param teamSeparation
 *        The fraction of the map used to separate teams.
 *        <ul>
 *          <li>{@code 2} means half the map separates teams</li>
 *          <li>{@code 4} means teams are separated by 25% of the map,
 *              allowing closer front spawns</li>
 *        </ul>
 * @param spawnMaskDeflate
 *        The distance from the edge of the spawnable layer within which
 *        spawn points are not allowed. The default value is {@code 4}.
 * @param clampMapHeight
 *        The maximum height allowed for the map.
 * @param fractalFlattenParams
 *        A list of flattening configurations applied to the fractal map,
 *        defining plateaus, ramps, and other height constraints.
 */
@Builder
public record FractalParams(
        @NonNull Float waterHeight,
        @NonNull FractalWaterMasks fractalWaterMask,
        @NonNull Integer noiseMapBlurAmount,
        @NonNull Integer noiseSmallestDetail,
        @NonNull Float noiseOctaveMultiplier,
        @NonNull Float noiseExpMultiplier,
        @NonNull Integer teamSeparation,
        @NonNull Integer spawnMaskDeflate,
        @NonNull Float clampMapHeight,
        @NonNull List<FractalFlattenParams> fractalFlattenParams
) {}