package com.faforever.neroxis.util;

public strictfp class MathUtil {

    public static float discretePercentage(float percent, int numBins) {
        return binPercentage(percent, numBins) / (float) numBins;
    }

    public static int binPercentage(float percent, int numBins) {
        return StrictMath.max(StrictMath.min(StrictMath.round(percent * numBins), numBins), 0);
    }

    public static float normalizeBin(int bin, int numBins) {
        return (float) bin / numBins;
    }

    public static float interpolate(float val0, float val1, float weight) {
        return (val1 - val0) * weight + val0;
    }

    public static float smoothStep(float val0, float val1, float weight) {
        return (val1 - val0) * (3.0f - weight * 2.0f) * weight * weight + val0;
    }

    public static float smootherStep(float val0, float val1, float weight) {
        return (val1 - val0) * ((weight * (weight * 6.0f - 15.0f) + 10.0f) * weight * weight * weight) + val0;
    }

    public static int binomialCoefficient(int n, int k) {
        float coefficient = 1;
        for (int i = 1; i <= k; ++i) {
            coefficient *= (float) (n + 1 - i) / i;
        }
        return (int) coefficient;
    }

    public static float log2(float val) {
        return (float) (StrictMath.log(val) / StrictMath.log(2));
    }
}
