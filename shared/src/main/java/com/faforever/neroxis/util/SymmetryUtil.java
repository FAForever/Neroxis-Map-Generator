package com.faforever.neroxis.util;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

public class SymmetryUtil {

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
            case X, QUAD, POINT4, POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14,
                 POINT15,
                 POINT16 -> {
                int halfSizeBound = size / 2 + size % 2;
                yield x -> x < halfSizeBound ? halfSizeBound : 0;
            }
            case ZX, DIAG -> x -> size - x;
            case Z, POINT2 -> {
                int halfSizeBound = size / 2 + size % 2;
                yield x -> halfSizeBound;
            }
            case NONE, XZ -> x -> size;
        };
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

    private static Vector2 getRotatedPoint(float x, float y, int size, float radians) {
        float halfSize = size / 2f;
        float xOffset = x - halfSize;
        float yOffset = y - halfSize;
        double cosAngle = StrictMath.cos(radians);
        double sinAngle = StrictMath.sin(radians);
        float newX = (float) (xOffset * cosAngle - yOffset * sinAngle + halfSize);
        float newY = (float) (xOffset * sinAngle + yOffset * cosAngle + halfSize);
        return new Vector2(newX, newY);
    }

}
