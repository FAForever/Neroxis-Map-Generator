package com.faforever.neroxis.map.mask;

import com.faforever.neroxis.map.SymmetrySettings;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public strictfp abstract class PrimitiveMask<T extends Comparable<T>, U extends ComparableMask<T, U>> extends ComparableMask<T, U> {

    public PrimitiveMask(int size, Long seed, SymmetrySettings symmetrySettings) {
        this(size, seed, symmetrySettings, null, false);
    }

    public PrimitiveMask(int size, Long seed, SymmetrySettings symmetrySettings, String name) {
        this(size, seed, symmetrySettings, name, false);
    }

    public PrimitiveMask(int size, Long seed, SymmetrySettings symmetrySettings, String name, boolean parallel) {
        super(size, seed, symmetrySettings, name, parallel);
    }

    public PrimitiveMask(U other, Long seed) {
        super(other, seed);
    }

    public PrimitiveMask(U other, Long seed, String name) {
        super(other, seed, name);
    }

    protected abstract int[][] getInnerCount();

    protected abstract T transformAverage(float value);

    @Override
    protected void maskFill(T value) {
        maskFill(mask, value);
    }

    @Override
    protected void maskFill(T[][] maskToFill, T value) {
        int maskSize = maskToFill.length;
        for (int r = 0; r < maskSize; ++r) {
            maskToFill[r][0] = value;
            for (int i = 1; i < maskSize; i += i) {
                System.arraycopy(maskToFill[r], 0, maskToFill[r], i, StrictMath.min((maskSize - i), i));
            }
        }
    }

    @Override
    protected void maskFill(T[][] maskToFill) {
        int maskSize = mask.length;
        assertSize(maskSize);
        for (int r = 0; r < maskSize; ++r) {
            System.arraycopy(mask[r], 0, maskToFill[r], 0, maskSize);
        }
    }

    public U blur(int radius) {
        enqueue(() -> {
            int[][] innerCount = getInnerCount();
            set((x, y) -> transformAverage(calculateAreaAverage(radius, x, y, innerCount)));
        });
        return (U) this;
    }

    public U blur(int radius, BooleanMask other) {
        enqueue(dependencies -> {
            BooleanMask limiter = (BooleanMask) dependencies.get(0);
            assertCompatibleMask(limiter);
            int[][] innerCount = getInnerCount();
            set((x, y) -> limiter.get(x, y) ? transformAverage(calculateAreaAverage(radius, x, y, innerCount)) : get(x, y));
        }, other);
        return (U) this;
    }

    protected void calculateInnerValue(int[][] innerCount, int x, int y, int val) {
        calculateScalarInnerValue(innerCount, x, y, val);
    }

    protected float calculateAreaAverage(int radius, int x, int y, int[][] innerCount) {
        int size = getSize();
        int xLeft = StrictMath.max(0, x - radius);
        int xRight = StrictMath.min(size - 1, x + radius);
        int yUp = StrictMath.max(0, y - radius);
        int yDown = StrictMath.min(size - 1, y + radius);
        int countA = xLeft > 0 && yUp > 0 ? innerCount[xLeft - 1][yUp - 1] : 0;
        int countB = yUp > 0 ? innerCount[xRight][yUp - 1] : 0;
        int countC = xLeft > 0 ? innerCount[xLeft - 1][yDown] : 0;
        int countD = innerCount[xRight][yDown];
        int count = countD + countA - countB - countC;
        int area = (xRight - xLeft + 1) * (yDown - yUp + 1);
        return (float) count / area;
    }

}
