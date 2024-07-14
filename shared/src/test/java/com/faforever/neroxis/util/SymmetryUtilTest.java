package com.faforever.neroxis.util;

import com.faforever.neroxis.map.Symmetry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
public class SymmetryUtilTest {

    @Test
    public void testMaxXBound() {
        int size = 256;
        int halfSize = size / 2;
        assertEquals(size, SymmetryUtil.getMaxXBound(Symmetry.NONE, size));
        assertEquals(size, SymmetryUtil.getMaxXBound(Symmetry.POINT2, size));
        assertEquals(size, SymmetryUtil.getMaxXBound(Symmetry.XZ, size));
        assertEquals(size, SymmetryUtil.getMaxXBound(Symmetry.ZX, size));
        assertEquals(size, SymmetryUtil.getMaxXBound(Symmetry.Z, size));
        assertEquals(size, SymmetryUtil.getMaxXBound(Symmetry.POINT3, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT4, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT5, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT6, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT7, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT9, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT10, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT8, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT11, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT12, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT13, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT14, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT15, size));
        assertEquals(halfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT16, size));
        int oddSize = size + 1;
        int oddHalfSize = halfSize + 1;
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.X, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.QUAD, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.DIAG, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT4, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT5, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT6, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT7, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT8, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT9, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT10, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT11, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT12, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT13, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT14, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT15, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.POINT16, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.X, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.QUAD, oddSize));
        assertEquals(oddHalfSize, SymmetryUtil.getMaxXBound(Symmetry.DIAG, oddSize));
    }

    @Test
    public void testMinYBoundFunction() {
        int size = 256;
        int halfSize = size / 2;
        int testPoint = halfSize + 1;
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.NONE, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT2, size).applyAsInt(testPoint));
        assertEquals(testPoint, SymmetryUtil.getMinYBoundFunction(Symmetry.XZ, size).applyAsInt(testPoint));
        assertEquals(testPoint, SymmetryUtil.getMinYBoundFunction(Symmetry.DIAG, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.ZX, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.Z, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT3, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT4, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT5, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT6, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT7, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT8, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT9, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT10, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT11, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT12, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT13, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT14, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT15, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMinYBoundFunction(Symmetry.POINT16, size).applyAsInt(testPoint));

        int dx = 10;
        testPoint = halfSize - dx;
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 5)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT5, size).applyAsInt(testPoint));
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 6)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT6, size).applyAsInt(testPoint));
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 7)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT7, size).applyAsInt(testPoint));
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 8)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT8, size).applyAsInt(testPoint));
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 9)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT9, size).applyAsInt(testPoint));
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 10)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT10, size).applyAsInt(testPoint));
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 11)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT11, size).applyAsInt(testPoint));
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 12)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT12, size).applyAsInt(testPoint));
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 13)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT13, size).applyAsInt(testPoint));
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 14)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT14, size).applyAsInt(testPoint));
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 15)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT15, size).applyAsInt(testPoint));
        assertEquals((int) (halfSize - StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 16)) * dx),
                     SymmetryUtil.getMinYBoundFunction(Symmetry.POINT16, size).applyAsInt(testPoint));
    }

    @Test
    public void testMaxYBoundFunction() {
        int size = 256;
        int halfSize = size / 2;
        int testPoint = halfSize;
        assertEquals(size, SymmetryUtil.getMaxYBoundFunction(Symmetry.NONE, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT2, size).applyAsInt(testPoint));
        assertEquals(size, SymmetryUtil.getMaxYBoundFunction(Symmetry.XZ, size).applyAsInt(testPoint));
        assertEquals(size, SymmetryUtil.getMaxYBoundFunction(Symmetry.X, size).applyAsInt(testPoint));
        assertEquals(size - testPoint, SymmetryUtil.getMaxYBoundFunction(Symmetry.DIAG, size).applyAsInt(testPoint));
        assertEquals(size - testPoint, SymmetryUtil.getMaxYBoundFunction(Symmetry.ZX, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.Z, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.QUAD, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT4, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT3, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT5, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT6, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT7, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT8, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT9, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT10, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT11, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT12, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT13, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT14, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT15, size).applyAsInt(testPoint));
        assertEquals(halfSize, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT16, size).applyAsInt(testPoint));

        int dx = 10;
        testPoint = halfSize + dx;
        assertEquals((int) (halfSize + StrictMath.tan(SymmetryUtil.convertToRotatedRadians(360f / 3)) * dx),
                     SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT3, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT5, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT6, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT7, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT8, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT9, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT10, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT11, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT12, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT13, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT14, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT15, size).applyAsInt(testPoint));
        assertEquals(0, SymmetryUtil.getMaxYBoundFunction(Symmetry.POINT16, size).applyAsInt(testPoint));
    }

}
