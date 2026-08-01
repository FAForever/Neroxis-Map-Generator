package com.faforever.neroxis.util.ops;

/**
 * Plain-loop implementation of {@link FloatArrayOps}. Used when the Vector API module is not
 * available at runtime, and as the reference implementation the vectorized variant is tested for
 * bitwise equality against.
 */
public final class ScalarFloatArrayOps implements FloatArrayOps {
    @Override
    public void add(float[] dst, float[] src, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] += src[i];
        }
    }

    @Override
    public void subtract(float[] dst, float[] src, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] -= src[i];
        }
    }

    @Override
    public void multiply(float[] dst, float[] src, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] *= src[i];
        }
    }

    @Override
    public void divide(float[] dst, float[] src, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] /= src[i];
        }
    }

    @Override
    public void add(float[] dst, float value, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] += value;
        }
    }

    @Override
    public void subtract(float[] dst, float value, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] -= value;
        }
    }

    @Override
    public void multiply(float[] dst, float value, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] *= value;
        }
    }

    @Override
    public void divide(float[] dst, float value, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] /= value;
        }
    }

    @Override
    public void sqrt(float[] dst, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] = (float) StrictMath.sqrt(dst[i]);
        }
    }

    @Override
    public void clampMin(float[] dst, float value, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] = Float.compare(dst[i], value) > 0 ? dst[i] : value;
        }
    }

    @Override
    public void clampMax(float[] dst, float value, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] = Float.compare(dst[i], value) < 0 ? dst[i] : value;
        }
    }

    @Override
    public void subtractMultiplyAdd(float[] dst, float subtrahend, float multiplier, float addend, int len) {
        for (int i = 0; i < len; i++) {
            dst[i] = (dst[i] - subtrahend) * multiplier + addend;
        }
    }

    @Override
    public float min(float[] array, int len) {
        float min = Float.POSITIVE_INFINITY;
        for (int i = 0; i < len; i++) {
            min = Math.min(min, array[i]);
        }
        return min;
    }

    @Override
    public float max(float[] array, int len) {
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < len; i++) {
            max = Math.max(max, array[i]);
        }
        return max;
    }

    @Override
    public void roundScaled(float[] src, int srcOffset, int[] dst, int dstOffset, float factor, int len) {
        for (int i = 0; i < len; i++) {
            dst[dstOffset + i] = StrictMath.round(src[srcOffset + i] * factor);
        }
    }

    @Override
    public void satBoxBlurRow(int[] sat, int aOffset, int bOffset, int cOffset, int dOffset, float area,
                              float postDivisor, float[] dst, int dstOffset, int len) {
        for (int i = 0; i < len; i++) {
            int count = sat[dOffset + i] + sat[aOffset + i] - sat[bOffset + i] - sat[cOffset + i];
            dst[dstOffset + i] = ((float) count / area) / postDivisor;
        }
    }
}
