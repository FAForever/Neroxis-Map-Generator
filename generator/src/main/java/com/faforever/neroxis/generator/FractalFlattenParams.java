package com.faforever.neroxis.generator;

import lombok.Builder;
import lombok.NonNull;

/**
 * Parameters controlling how a fractal heightmap region is flattened and mapped
 * into a destination height range.
 *
 * @param minHeight            The minimum height from the source data to flatten.
 * @param maxHeight            The maximum height from the source data to flatten.
 * @param destinationMinHeight The minimum height to use in the destination map.
 * @param destinationMaxHeight The maximum height to use in the destination map.
 *                             If the minimum and maximum destination heights are the same,
 *                             the resulting region will be completely flat.
 * @param slope                The slope to apply when {@code destinationMaxHeight > destinationMinHeight}.
 *                             Values greater than {@code 1.0} produce an exponential slope.
 * @param edgeBlur             The amount of blur to apply to the edge of the layer, used to smooth
 *                             the edges of plateaus. A value of {@code 0} applies no blur.
 * @param hasRamps             {@code true} if pathable ramps should be generated to the lower layer.
 * @param rampPercentage       The percentage of the edge for the layer, that should be ramps, 0.0 to 1.0
 * @param spawnable            {@code true} if spawn points are allowed to generate on this layer.
 * @param spawnMaskDeflate     The distance from the edge of the layer within which players
 *                             are not allowed to spawn.
 */
@Builder
public record FractalFlattenParams(
        Float minHeight,
        Float maxHeight,
        Float destinationMinHeight,
        Float destinationMaxHeight,
        Float slope,
        Integer edgeBlur,
        Boolean hasRamps,
        Float rampPercentage,
        Boolean spawnable,
        Integer spawnMaskDeflate
) {}
