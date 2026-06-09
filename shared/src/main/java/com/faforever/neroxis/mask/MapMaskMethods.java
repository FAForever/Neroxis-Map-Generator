package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("UnusedReturnValue")
public class MapMaskMethods {
    private MapMaskMethods() {
    }

    public static BooleanMask connectLocationsThroughMiddle(List<Vector2> locations, long seed, BooleanMask exec,
                                                            int minMiddlePoints, int maxMiddlePoints,
                                                            int numConnections,
                                                            float maxStepSize) {
        Random random = new Random(seed);
        for (int i = 0; i < numConnections; ++i) {
            int numMiddlePoints;
            if (maxMiddlePoints > minMiddlePoints) {
                numMiddlePoints = random.nextInt(maxMiddlePoints - minMiddlePoints) + minMiddlePoints;
            } else {
                numMiddlePoints = maxMiddlePoints;
            }
            Vector2 start = locations.get(random.nextInt(locations.size()));
            float maxMiddleDistance = start.getDistance(start);
            exec.connect(start, start, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2,
                         (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
        }
        return exec;
    }

    public static BooleanMask connectThroughCenter(List<Vector2> locations, long seed, BooleanMask exec,
                                                   int minMiddlePoints, int maxMiddlePoints,
                                                   int numConnections, float maxStepSize) {
        Random random = new Random(seed);
        Vector2 center = new Vector2(exec.getSize() / 2f, exec.getSize() / 2f);
        for (int i = 0; i < numConnections; ++i) {
            int numMiddlePoints;
            if (maxMiddlePoints > minMiddlePoints) {
                numMiddlePoints = random.nextInt(maxMiddlePoints - minMiddlePoints) + minMiddlePoints;
            } else {
                numMiddlePoints = maxMiddlePoints;
            }

            Vector2 start = locations.get(random.nextInt(locations.size()));
            Vector2 end = locations.get(random.nextInt(locations.size()));
            if (locations.size() > 1) {
                while (end.equals(start)) {
                    end = locations.get(random.nextInt(locations.size()));
                }
            }

            int firstHalf = numMiddlePoints / 2;
            int secondHalf = numMiddlePoints - firstHalf;

            float maxMiddleDistance1 = start.getDistance(center) / Math.max(1, firstHalf) * 2;
            float maxMiddleDistance2 = center.getDistance(end) / Math.max(1, secondHalf) * 2;

            exec.connect(start, center, maxStepSize, firstHalf, maxMiddleDistance1, maxMiddleDistance1 / 2,
                         (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
            exec.connect(center, end, maxStepSize, secondHalf, maxMiddleDistance2, maxMiddleDistance2 / 2,
                         (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
        }
        return exec;
    }

    public static BooleanMask connectLocationsAroundCenter(List<Vector2> locations, long seed, BooleanMask exec,
                                                           int minMiddlePoints, int maxMiddlePoints, int numConnections,
                                                           float maxStepSize, int bound) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            for (int i = 0; i < numConnections; ++i) {
                int numMiddlePoints;
                if (maxMiddlePoints > minMiddlePoints) {
                    numMiddlePoints = random.nextInt(maxMiddlePoints - minMiddlePoints) + minMiddlePoints;
                } else {
                    numMiddlePoints = maxMiddlePoints;
                }
                Vector2 start = locations.get(random.nextInt(locations.size()));
                float offCenterAngle = (float) (StrictMath.PI * (1f / 3f + random.nextFloat() / 3f));
                offCenterAngle *= random.nextBoolean() ? 1 : -1;
                offCenterAngle += start.angleTo(new Vector2(exec.getSize() / 2f, exec.getSize() / 2f));
                Vector2 end = start.addPolar(offCenterAngle,
                                             random.nextFloat() * exec.getSize() / 2f + exec.getSize() / 2f)
                                   .clampMax(exec.getSize() - bound)
                                   .clampMin(bound);
                float maxMiddleDistance = start.getDistance(end);
                exec.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2,
                             (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
            }
        });
    }

    public static BooleanMask connectLocations(List<Vector2> locations, long seed, BooleanMask exec,
                                               int maxMiddlePoints, int numConnections, float maxStepSize) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            if (locations.size() > 1) {
                locations.forEach(startSpawn -> {
                    for (int i = 0; i < numConnections; ++i) {
                        ArrayList<Vector2> otherSpawns = new ArrayList<>(locations);
                        otherSpawns.remove(startSpawn);
                        Vector2 endSpawn = otherSpawns.get(random.nextInt(otherSpawns.size()));
                        int numMiddlePoints = random.nextInt(maxMiddlePoints);
                        float maxMiddleDistance = startSpawn.getDistance(endSpawn) / numMiddlePoints * 2;
                        exec.path(startSpawn, endSpawn, maxStepSize, numMiddlePoints, maxMiddleDistance, 0,
                                  (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
                    }
                });
            }
        });
    }

    public static BooleanMask pathInCenterBounds(long seed, BooleanMask exec, float maxStepSize, int numPaths,
                                                 int maxMiddlePoints, int bound, float maxAngleError) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            for (int i = 0; i < numPaths; i++) {
                Vector2 start = new Vector2(random.nextInt(exec.getSize() + 1 - bound * 2) + bound,
                                            random.nextInt(exec.getSize() + 1 - bound * 2) + bound);
                Vector2 end = new Vector2(random.nextInt(exec.getSize() + 1 - bound * 2) + bound,
                                          random.nextInt(exec.getSize() + 1 - bound * 2) + bound);
                int numMiddlePoints = random.nextInt(maxMiddlePoints);
                float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
                exec.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError,
                          SymmetryType.TERRAIN);
            }
        });
    }

    public static BooleanMask pathInEdgeBounds(long seed, BooleanMask exec, float maxStepSize, int numPaths,
                                               int maxMiddlePoints, int bound, float maxAngleError) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            for (int i = 0; i < numPaths; i++) {
                int startX = random.nextInt(bound) + (random.nextBoolean() ? 0 : exec.getSize() - bound);
                int startY = random.nextInt(bound) + (random.nextBoolean() ? 0 : exec.getSize() - bound);
                int endX = random.nextInt(bound * 2) - bound + startX;
                int endY = random.nextInt(bound * 2) - bound + startY;
                Vector2 start = new Vector2(startX, startY);
                Vector2 end = new Vector2(endX, endY);
                int numMiddlePoints = random.nextInt(maxMiddlePoints);
                float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
                exec.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError,
                          SymmetryType.TERRAIN);
            }
        });
    }

    public static BooleanMask pathAroundLocations(List<Vector2> locations, long seed, BooleanMask exec,
                                                  float maxStepSize, int numPaths, int maxMiddlePoints, int bound,
                                                  float maxAngleError) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            locations.forEach(location -> {
                for (int i = 0; i < numPaths; i++) {
                    int endX = (int) (random.nextFloat(bound) + location.x());
                    int endY = (int) (random.nextFloat(bound) + location.y());
                    Vector2 end = new Vector2(endX, endY);
                    int numMiddlePoints = random.nextInt(maxMiddlePoints);
                    float maxMiddleDistance = location.getDistance(end) / numMiddlePoints * 2;
                    exec.path(location, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError,
                              SymmetryType.SPAWN);
                }
            });
        });
    }

    /**
     * Flattens a height band in the terrain by remapping values within the specified height range
     * to a destination height range using a slope-based curve.
     *
     * @param exec the FloatMask to modify
     * @param noiseMap the noise map used to determine which areas to flatten
     * @param minHeight the minimum height of the band to flatten
     * @param maxHeight the maximum height of the band to flatten
     * @param destinationMinHeight the minimum height in the destination range
     * @param destinationMaxHeight the maximum height in the destination range
     * @param slope = 1 → linear interpolation.
     *              > 1 → slower start, faster rise.
     *              < 1 → faster start, slower rise.
     *              ≤ 0 → uses destinationMaxHeight for entire band.
     * @return the modified FloatMask     */
    public static FloatMask flattenHeightBand(FloatMask exec, FloatMask noiseMap, float minHeight, float maxHeight,
                                              float destinationMinHeight, float destinationMaxHeight, float slope) {
        return exec.enqueue(dependencies -> {
           FloatMask noise = (FloatMask) dependencies.getFirst();
           BooleanMask flattenMask = noise.copyAsBooleanMask(minHeight, maxHeight);
           exec.setPrimitiveWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
               float value = noise.getPrimitive(x, y);
               if (flattenMask.getPrimitive(x, y)) {
                   if (slope <= 0 || maxHeight <= minHeight) {
                        return destinationMaxHeight;
                   } else {
                       return remapWithSlope(value, minHeight, maxHeight, destinationMinHeight, destinationMaxHeight, slope);
                   }
               } else {
                   return exec.getPrimitive(x, y);
               }
           });
        }, noiseMap);
    }

    private static float remapWithSlope(float value, float minHeight, float maxHeight,
                                        float destinationMinHeight, float destinationMaxHeight,
                                        float slope) {
        // Handle edge case where height range is zero
        if (maxHeight <= minHeight) {
            return destinationMaxHeight;
        }

        // Normalize value to 0–1 range
        float normalized = (value - minHeight) / (maxHeight - minHeight);

        // Clamp to stay within bounds
        normalized = StrictMath.max(0f, StrictMath.min(1f, normalized));

        // Apply slope for non-linear curve
        float curved = (float) StrictMath.pow(normalized, slope);

        // Map to destination range
        return destinationMinHeight + (destinationMaxHeight - destinationMinHeight) * curved;
    }
}
