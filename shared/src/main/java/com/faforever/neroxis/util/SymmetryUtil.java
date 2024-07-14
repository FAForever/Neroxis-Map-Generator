package com.faforever.neroxis.util;

import com.faforever.neroxis.map.Symmetry;

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
                    if (x <= halfSizeBound) {
                        return halfSizeBound;
                    }

                    float dx = x - halfSize;
                    int y = (int) (halfSize + tan * dx);
                    return MathUtil.clamp(y, 0, halfSizeBound);
                };
            }
            case POINT5, POINT6, POINT7, POINT8, POINT9, POINT10, POINT11, POINT12, POINT13, POINT14, POINT15,
                 POINT16 -> {
                int halfSizeBound = size / 2 + size % 2;
                yield x -> x <= halfSizeBound ? halfSizeBound : 0;
            }
            case ZX, DIAG -> x -> size - x;
            case Z, POINT2, POINT4, QUAD -> {
                int maxY = size / 2 + size % 2;
                yield x -> maxY;
            }
            case X, NONE, XZ -> x -> size;
        };
    }

    static double convertToRotatedRadians(float angle) {
        // For the map generator the 0 angle points to the left, so we have to adjust all angles by 180 degrees
        float adjustedAngle = angle + 180;
        return (adjustedAngle / 180) * StrictMath.PI;
    }

}
