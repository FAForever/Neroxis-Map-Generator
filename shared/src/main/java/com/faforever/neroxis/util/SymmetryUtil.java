package com.faforever.neroxis.util;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.util.functional.SymmetryRegionBoundsChecker;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class SymmetryUtil {

    public static SymmetryRegionBoundsChecker getSymmetryRegionBoundsChecker(Symmetry symmetry, int size) {
        int maxXBound = getMaxXBound(symmetry, size);
        IntUnaryOperator minYBoundFunction = getMinYBoundFunction(symmetry, size);
        IntUnaryOperator maxYBoundFunction = getMaxYBoundFunction(symmetry, size);
        return (x, y) -> x >= 0
                         && x < maxXBound
                         && y >= minYBoundFunction.applyAsInt(x)
                         && y < maxYBoundFunction.applyAsInt(x);
    }

    public static int getMaxXBound(Symmetry symmetry, int size) {
        return switch (symmetry) {
            case POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15,
                 POINT16, X, QUAD, DIAG -> size / 2 + size % 2;
            case POINT2, POINT3, XZ, ZX, Z, NONE -> size;
        };
    }

    public static IntUnaryOperator getMinYBoundFunction(Symmetry symmetry, int size) {
        return switch (symmetry) {
            case POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15,
                 POINT16 -> {
                double radians = convertToRotatedRadians(360f / symmetry.getNumSymPoints());
                double tan = StrictMath.tan(radians);
                int halfSizeBound = size / 2 + size % 2;
                float halfSize = size / 2f;
                yield x -> {
                    if (x > halfSizeBound) {
                        return 0;
                    }

                    float dx = x - halfSize;
                    int y = (int) (halfSize + tan * dx);
                    return MathUtil.clamp(y, 0, size);
                };
            }
            case DIAG, XZ -> x -> x;
            case POINT2, POINT3, POINT4, ZX, X, Z, QUAD, NONE -> x -> 0;
        };
    }

    public static IntUnaryOperator getMaxYBoundFunction(Symmetry symmetry, int size) {
        return switch (symmetry) {
            case POINT3 -> {
                double tan = StrictMath.tan(convertToRotatedRadians(360f / symmetry.getNumSymPoints()));
                int halfSizeBound = size / 2 + size % 2;
                float halfSize = size / 2f;
                yield x -> {
                    //The max Y in the first quadrant is always the halfway point
                    if (x < halfSizeBound) {
                        return halfSizeBound;
                    }

                    float dx = x - halfSize;
                    int y = (int) (halfSize + tan * dx);
                    return MathUtil.clamp(y, 0, halfSizeBound);
                };
            }
            case QUAD, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14,
                 POINT15, POINT16 -> {
                int halfSizeBound = size / 2 + size % 2;
                yield x -> x < halfSizeBound ? halfSizeBound : 0;
            }
            case ZX, DIAG -> x -> size - x;
            case Z, POINT2 -> {
                int halfSizeBound = size / 2 + size % 2;
                yield x -> halfSizeBound;
            }
            case NONE, X, XZ -> x -> size;
        };
    }

    public static List<Vector2> getRandomPointsInBounds(RandomGenerator random, Symmetry symmetry, int size,
                                                        int numPoints) {
        int maxX = getMaxXBound(symmetry, size);
        IntUnaryOperator minYBoundFunction = getMinYBoundFunction(symmetry, size);
        IntUnaryOperator maxYBoundFunction = getMaxYBoundFunction(symmetry, size);
        return IntStream.range(0, numPoints).mapToObj(i -> {
            int x = random.nextInt(0, maxX);
            int minY = minYBoundFunction.applyAsInt(x);
            int maxY = maxYBoundFunction.applyAsInt(x);
            int y = minY >= maxY ? minY : random.nextInt(minYBoundFunction.applyAsInt(x),
                                                         maxYBoundFunction.applyAsInt(x));
            return new Vector2(x, y);
        }).toList();
    }

    static double convertToRotatedRadians(float angle) {
        // For the map generator the 0 angle points to the left, so we have to adjust all angles by 180 degrees
        float adjustedAngle = angle + 180;
        return (adjustedAngle / 180) * StrictMath.PI;
    }

    public static List<Vector2> getSymmetryPoints(float x, float y, int size, Symmetry symmetry,
                                                  Symmetry secondarySymmetry) {
        int numSymPoints = symmetry.getNumSymPoints();
        return switch (symmetry) {
            case NONE -> List.of();
            case POINT2 -> List.of(new Vector2(size - x - 1, size - y - 1));
            case POINT4 -> List.of(new Vector2(size - x - 1, size - y - 1), new Vector2(y, size - x - 1),
                                   new Vector2(size - y - 1, x));
            case POINT6, POINT8, POINT10, POINT12, POINT14, POINT16 -> {
                List<Vector2> symmetryPoints = new ArrayList<>(numSymPoints - 1);
                symmetryPoints.add(new Vector2(size - x - 1, size - y - 1));
                for (int i = 1; i < numSymPoints / 2; i++) {
                    float angle = (float) (2 * StrictMath.PI * i / numSymPoints);
                    Vector2 rotated = getRotatedPoint(x, y, size, angle);
                    symmetryPoints.add(rotated);
                    Vector2 antiRotated = getRotatedPoint(x, y, size, (float) (angle + StrictMath.PI));
                    symmetryPoints.add(antiRotated);
                }
                yield symmetryPoints;
            }
            case POINT3, POINT5, POINT7, POINT9, POINT11, POINT13, POINT15 -> {
                List<Vector2> symmetryPoints = new ArrayList<>(numSymPoints - 1);
                for (int i = 1; i < numSymPoints; i++) {
                    Vector2 rotated = getRotatedPoint(x, y, size, (float) (2 * StrictMath.PI * i / numSymPoints));
                    symmetryPoints.add(rotated);
                }
                yield symmetryPoints;
            }
            case X -> List.of(new Vector2(size - x - 1, y));
            case Z -> List.of(new Vector2(x, size - y - 1));
            case XZ -> List.of(new Vector2(y, x));
            case ZX -> List.of(new Vector2(size - y - 1, size - x - 1));
            case QUAD -> {
                if (secondarySymmetry == Symmetry.Z) {
                    yield List.of(new Vector2(x, size - y - 1), new Vector2(size - x - 1, y),
                                  new Vector2(size - x - 1, size - y - 1));
                } else {
                    yield List.of(new Vector2(size - x - 1, y), new Vector2(x, size - y - 1),
                                  new Vector2(size - x - 1, size - y - 1));
                }
            }
            case DIAG -> {
                if (secondarySymmetry == Symmetry.ZX) {
                    yield List.of(new Vector2(size - y - 1, size - x - 1), new Vector2(y, x),
                                  new Vector2(size - x - 1, size - y - 1));
                } else {
                    yield List.of(new Vector2(y, x), new Vector2(size - y - 1, size - x - 1),
                                  new Vector2(size - x - 1, size - y - 1));
                }
            }
        };
    }

    public static List<Float> getSymmetryRotations(float rot, Symmetry symmetry, Symmetry secondarySymmetry) {
        List<Float> symmetryRotation = new ArrayList<>();
        final float xRotation = (float) StrictMath.atan2(-StrictMath.sin(rot), StrictMath.cos(rot));
        final float zRotation = (float) StrictMath.atan2(-StrictMath.cos(rot), StrictMath.sin(rot));
        final float diagRotation = (float) StrictMath.atan2(-StrictMath.cos(rot), -StrictMath.sin(rot));
        switch (symmetry) {
            case POINT2, X, Z -> symmetryRotation.add(rot + (float) StrictMath.PI);
            case POINT4 -> {
                symmetryRotation.add(rot + (float) StrictMath.PI);
                symmetryRotation.add(rot + (float) StrictMath.PI / 2);
                symmetryRotation.add(rot - (float) StrictMath.PI / 2);
            }
            case POINT3, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15,
                 POINT16 -> {
                int numSymPoints = symmetry.getNumSymPoints();
                for (int i = 1; i < numSymPoints; i++) {
                    symmetryRotation.add(rot + (float) (2 * StrictMath.PI * i / numSymPoints));
                }
            }
            case XZ, ZX -> symmetryRotation.add(diagRotation);
            case QUAD -> {
                if (secondarySymmetry == Symmetry.Z) {
                    symmetryRotation.add(zRotation);
                    symmetryRotation.add(xRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                } else {
                    symmetryRotation.add(xRotation);
                    symmetryRotation.add(zRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                }
            }
            case DIAG -> {
                if (secondarySymmetry == Symmetry.ZX) {
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                } else {
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(diagRotation);
                    symmetryRotation.add(rot + (float) StrictMath.PI);
                }
            }
        }
        return symmetryRotation;
    }

    public static Vector2 getSourcePoint(int x, int y, int size, Symmetry symmetry) {
        int halfSizeBound = size / 2 + size % 2;
        return switch (symmetry) {
            case NONE -> new Vector2(x, y);
            case POINT2 -> {
                if (y >= halfSizeBound) {
                    yield new Vector2(size - x - 1, size - y - 1);
                } else {
                    yield new Vector2(x, y);
                }
            }
            case POINT4 -> {
                if (x >= halfSizeBound && y >= halfSizeBound) {
                    yield new Vector2(size - x - 1, size - y - 1);
                } else if (x <= halfSizeBound && y >= halfSizeBound) {
                    yield new Vector2(size - y - 1, x);
                } else if (x >= halfSizeBound) {
                    yield new Vector2(y, size - x - 1);
                } else {
                    yield new Vector2(x, y);
                }
            }
            case POINT6, POINT8, POINT10, POINT12, POINT14, POINT16, POINT3, POINT5, POINT7, POINT9, POINT11, POINT13,
                 POINT15 -> {
                float baseRadians = (float) (StrictMath.PI * 2f / symmetry.getNumSymPoints());
                float dx = x - (size / 2f);
                float dy = y - (size / 2f);

                float angle = (float) (StrictMath.atan2(dy, dx) + StrictMath.PI);

                float rawSlice = angle / baseRadians;
                int slice = (int) rawSlice;
                if (rawSlice == slice) {
                    slice--;
                }
                if (slice == 0) {
                    yield new Vector2(x, y);
                } else {
                    float antiRotateAngle = -slice * baseRadians;
                    yield getRotatedPoint(x, y, size, antiRotateAngle);
                }
            }
            case X -> {
                if (x >= halfSizeBound) {
                    yield new Vector2(size - x - 1, y);
                } else {
                    yield new Vector2(x, y);
                }
            }
            case Z -> {
                if (y >= halfSizeBound) {
                    yield new Vector2(x, size - y - 1);
                } else {
                    yield new Vector2(x, y);
                }
            }
            case XZ -> {
                if (x > y) {
                    yield new Vector2(y, x);
                } else {
                    yield new Vector2(x, y);
                }
            }
            case ZX -> {
                if (y > size - x - 1) {
                    yield new Vector2(size - y - 1, size - x - 1);
                } else {
                    yield new Vector2(x, y);
                }
            }
            case QUAD -> {
                if (x >= halfSizeBound) {
                    if (y >= halfSizeBound) {
                        yield new Vector2(size - x - 1, size - y - 1);
                    } else {
                        yield new Vector2(size - x - 1, y);
                    }
                } else {
                    if (y >= halfSizeBound) {
                        yield new Vector2(x, size - y - 1);
                    } else {
                        yield new Vector2(x, y);
                    }
                }
            }
            case DIAG -> {
                if (x > y) {
                    if (y > size - x - 1) {
                        yield new Vector2(size - x - 1, size - y - 1);
                    } else {
                        yield new Vector2(y, x);
                    }
                } else {
                    if (y > size - x - 1) {
                        yield new Vector2(size - y - 1, size - x - 1);
                    } else {
                        yield new Vector2(x, y);
                    }
                }
            }
        };
    }

    public static Vector2 getRotatedPoint(float x, float y, int size, float radians) {
        if (radians == 0) {
            return new Vector2(x, y);
        }
        float halfSize = size / 2f - .5f;

        // Translate so that center is at origin
        float xt = x - halfSize;
        float yt = y - halfSize;

        Vector2 result = rotateByShear(radians, xt, yt);

        // Translate back
        return new Vector2(result.x() + halfSize, result.y() + halfSize);
    }

    public static Vector2 rotateByShear(float radians, float xt, float yt) {
        float sign = StrictMath.signum(radians);
        if (sign == 0) {
            return new Vector2(xt, yt);
        }

        float xs;
        float ys;
        float halfPi = (float) (StrictMath.PI / 2);
        int numNinetyDegreeTurns = StrictMath.round(radians / halfPi);
        switch (numNinetyDegreeTurns % 4) {
            case -3, 1 -> {
                xs = -yt;
                ys = xt;
            }
            case -2, 2 -> {
                xs = -xt;
                ys = -yt;
            }
            case -1, 3 -> {
                xs = yt;
                ys = -xt;
            }
            case 0 -> {
                xs = xt;
                ys = yt;
            }
            default -> throw new IllegalStateException("Unexpected number of 90 degree turns");
        }

        float residualRadians = radians - numNinetyDegreeTurns * halfPi;
        if (residualRadians == 0) {
            return new Vector2(xs, ys);
        }

        float tanHalf = (float) StrictMath.tan(residualRadians / 2.0);
        float sin = (float) StrictMath.sin(residualRadians);

        // Step 1: shear along x-axis
        float x1 = StrictMath.round(xs - ys * tanHalf);
        float y1 = StrictMath.round(ys);

        // Step 2: shear along y-axis
        float x2 = StrictMath.round(x1);
        float y2 = StrictMath.round(y1 + x1 * sin);

        // Step 3: shear along x-axis again
        float xr = StrictMath.round(x2 - y2 * tanHalf);
        float yr = StrictMath.round(y2);
        return new Vector2(xr, yr);
    }
}
