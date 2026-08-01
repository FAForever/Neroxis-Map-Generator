package com.faforever.neroxis.util.ops;

/**
 * Bulk operations over flat float arrays — the single hot-loop seam behind {@code FloatMask}.
 * Implementations must be bit-identical to each other for every input so that generated maps do
 * not depend on which implementation a machine selects (multiplayer requires the same seed to
 * produce the same map everywhere). Consequently only operations whose vectorized form is exactly
 * IEEE-equivalent to the scalar form belong here: per-element add/subtract/multiply/divide,
 * sqrt (correctly rounded), blend-based clamps, and min/max reductions (fully associative and
 * commutative, so lane width cannot change the result).
 *
 * <p>Deliberately absent: floating-point sum (non-associative — a vector reduction is not even
 * deterministic across different CPUs' lane widths) and transcendentals like pow/exp/log (their
 * vector counterparts are not correctly rounded and differ across platforms). Those must stay
 * scalar in fixed iteration order.
 *
 * <p>Clamp values must not be NaN, and {@code -0.0f} clamp values are not distinguished from
 * {@code +0.0f}.
 */
public interface FloatArrayOps {
    /** {@code dst[i] += src[i]} for {@code i in [0, len)} */
    void add(float[] dst, float[] src, int len);

    /** {@code dst[i] -= src[i]} for {@code i in [0, len)} */
    void subtract(float[] dst, float[] src, int len);

    /** {@code dst[i] *= src[i]} for {@code i in [0, len)} */
    void multiply(float[] dst, float[] src, int len);

    /** {@code dst[i] /= src[i]} for {@code i in [0, len)} */
    void divide(float[] dst, float[] src, int len);

    /** {@code dst[i] += value} for {@code i in [0, len)} */
    void add(float[] dst, float value, int len);

    /** {@code dst[i] -= value} for {@code i in [0, len)} */
    void subtract(float[] dst, float value, int len);

    /** {@code dst[i] *= value} for {@code i in [0, len)} */
    void multiply(float[] dst, float value, int len);

    /** {@code dst[i] /= value} for {@code i in [0, len)} */
    void divide(float[] dst, float value, int len);

    /** {@code dst[i] = (float) sqrt(dst[i])}, IEEE correctly rounded */
    void sqrt(float[] dst, int len);

    /** {@code dst[i] = max(dst[i], value)} with {@code Float.compare} ordering for NaN elements */
    void clampMin(float[] dst, float value, int len);

    /** {@code dst[i] = min(dst[i], value)} with {@code Float.compare} ordering for NaN elements */
    void clampMax(float[] dst, float value, int len);

    /**
     * {@code dst[i] = (dst[i] - subtrahend) * multiplier + addend}, evaluated in exactly that
     * order with no fused multiply-add
     */
    void subtractMultiplyAdd(float[] dst, float subtrahend, float multiplier, float addend, int len);

    /** Minimum with {@link Math#min(float, float)} semantics (NaN-propagating, -0.0 &lt; +0.0) */
    float min(float[] array, int len);

    /** Maximum with {@link Math#max(float, float)} semantics (NaN-propagating, -0.0 &lt; +0.0) */
    float max(float[] array, int len);

    /**
     * {@code dst[dstOffset+i] = StrictMath.round(src[srcOffset+i] * factor)} (round half up with
     * exact tie handling, matching {@link Math#round(float)} bit for bit)
     */
    void roundScaled(float[] src, int srcOffset, int[] dst, int dstOffset, float factor, int len);

    /**
     * One row of a summed-area-table box blur with the four corner lookups at fixed offsets:
     * {@code dst[dstOffset+i] = ((float) (sat[dOffset+i] + sat[aOffset+i] - sat[bOffset+i]
     * - sat[cOffset+i]) / area) / postDivisor} — the divisions performed in exactly that order.
     */
    void satBoxBlurRow(int[] sat, int aOffset, int bOffset, int cOffset, int dOffset, float area, float postDivisor,
                       float[] dst, int dstOffset, int len);
}
