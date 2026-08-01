package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetrySettings;
import org.jspecify.annotations.Nullable;

import java.util.random.RandomGenerator;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public abstract sealed class PrimitiveMask<T extends Comparable<T>, U extends ComparableMask<T, U>> extends
                                                                                                    ComparableMask<T, U> permits
                                                                                                                         BooleanMask,
                                                                                                                         FloatMask,
                                                                                                                         IntegerMask {
    public PrimitiveMask(int size, RandomGenerator.@Nullable SplittableGenerator random, SymmetrySettings symmetrySettings,
                         @Nullable String name) {
        super(size, random, symmetrySettings, name);
    }

    protected PrimitiveMask(U other, @Nullable String name) {
        super(other, name);
    }

    /**
     * Summed-area table over the mask's quantized values, padded with a leading zero row and
     * column: flat row-major {@code (x + 1) * (size + 1) + (y + 1)} holds the inclusive prefix sum
     * of {@code values[0..x][0..y]}. The padding makes the 4-corner area lookups branch-free.
     */
    protected abstract int[] getInnerCount();

    /**
     * In-place 2D inclusive prefix sum over padded flat storage as described in
     * {@link #getInnerCount()}. Callers fill the quantized values at the padded positions and
     * leave row 0 and column 0 zeroed. Integer math, so the result is exactly the same as the
     * previous per-pixel recurrence regardless of traversal order.
     */
    protected static void prefixSum2DPadded(int[] values, int size) {
        int stride = size + 1;
        for (int x = 1; x <= size; x++) {
            int base = x * stride;
            int rowSum = 0;
            for (int y = 1; y <= size; y++) {
                rowSum += values[base + y];
                values[base + y] = rowSum + values[base - stride + y];
            }
        }
    }

    protected float calculateAreaAverageAsInts(int radius, int x, int y, int[] innerCount) {
        int size = getSize();
        int stride = size + 1;
        int xLeft = StrictMath.max(0, x - radius);
        int xRight = StrictMath.min(size - 1, x + radius);
        int yUp = StrictMath.max(0, y - radius);
        int yDown = StrictMath.min(size - 1, y + radius);
        int countA = innerCount[xLeft * stride + yUp];
        int countB = innerCount[(xRight + 1) * stride + yUp];
        int countC = innerCount[xLeft * stride + (yDown + 1)];
        int countD = innerCount[(xRight + 1) * stride + (yDown + 1)];
        int count = countD + countA - countB - countC;
        int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
        return (float) count / area;
    }
}
