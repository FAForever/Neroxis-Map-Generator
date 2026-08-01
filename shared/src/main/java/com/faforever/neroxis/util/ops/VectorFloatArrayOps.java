package com.faforever.neroxis.util.ops;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import static jdk.incubator.vector.VectorOperators.F2I;
import static jdk.incubator.vector.VectorOperators.I2F;

/**
 * SIMD implementation of {@link FloatArrayOps} using the Vector API. Every method is bit-identical
 * to {@link ScalarFloatArrayOps} for all inputs (verified by {@code FloatArrayOpsEquivalenceTest}):
 * only per-lane IEEE operations are used, never FMA, and the clamps reproduce the exact
 * {@code Float.compare} branch decision including NaN elements and {@code -0.0f}/{@code +0.0f}
 * ties. Only loaded (reflectively, by {@link FloatArrayOpsHolder}) when the
 * {@code jdk.incubator.vector} module is present at runtime.
 */
public final class VectorFloatArrayOps implements FloatArrayOps {
    private static final VectorSpecies<Float> F = FloatVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> I = IntVector.SPECIES_PREFERRED;

    @Override
    public void add(float[] dst, float[] src, int len) {
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            FloatVector.fromArray(F, dst, i).add(FloatVector.fromArray(F, src, i)).intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] += src[i];
        }
    }

    @Override
    public void subtract(float[] dst, float[] src, int len) {
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            FloatVector.fromArray(F, dst, i).sub(FloatVector.fromArray(F, src, i)).intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] -= src[i];
        }
    }

    @Override
    public void multiply(float[] dst, float[] src, int len) {
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            FloatVector.fromArray(F, dst, i).mul(FloatVector.fromArray(F, src, i)).intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] *= src[i];
        }
    }

    @Override
    public void divide(float[] dst, float[] src, int len) {
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            FloatVector.fromArray(F, dst, i).div(FloatVector.fromArray(F, src, i)).intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] /= src[i];
        }
    }

    @Override
    public void add(float[] dst, float value, int len) {
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            FloatVector.fromArray(F, dst, i).add(value).intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] += value;
        }
    }

    @Override
    public void subtract(float[] dst, float value, int len) {
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            FloatVector.fromArray(F, dst, i).sub(value).intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] -= value;
        }
    }

    @Override
    public void multiply(float[] dst, float value, int len) {
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            FloatVector.fromArray(F, dst, i).mul(value).intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] *= value;
        }
    }

    @Override
    public void divide(float[] dst, float value, int len) {
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            FloatVector.fromArray(F, dst, i).div(value).intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] /= value;
        }
    }

    @Override
    public void sqrt(float[] dst, int len) {
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            // Lanewise SQRT is IEEE correctly rounded, identical to (float) StrictMath.sqrt(double)
            FloatVector.fromArray(F, dst, i).sqrt().intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] = (float) StrictMath.sqrt(dst[i]);
        }
    }

    @Override
    public void clampMin(float[] dst, float value, int len) {
        // Keep the element when Float.compare(element, value) > 0: strictly greater, or NaN
        // (compares above any non-NaN value), or numerically equal with greater int bits
        // (distinguishes +0.0 from -0.0 exactly like Float.compare).
        int valueBits = Float.floatToIntBits(value);
        FloatVector broadcastValue = FloatVector.broadcast(F, value);
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            FloatVector v = FloatVector.fromArray(F, dst, i);
            VectorMask<Float> keep = v.compare(VectorOperators.GT, value)
                                      .or(v.test(VectorOperators.IS_NAN))
                                      .or(v.compare(VectorOperators.EQ, value)
                                           .and(v.reinterpretAsInts()
                                                 .compare(VectorOperators.GT, valueBits)
                                                 .cast(F)));
            broadcastValue.blend(v, keep).intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] = Float.compare(dst[i], value) > 0 ? dst[i] : value;
        }
    }

    @Override
    public void clampMax(float[] dst, float value, int len) {
        // Keep the element when Float.compare(element, value) < 0; NaN elements never compare
        // below, so they take the clamp value — matching the scalar branch exactly.
        int valueBits = Float.floatToIntBits(value);
        FloatVector broadcastValue = FloatVector.broadcast(F, value);
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            FloatVector v = FloatVector.fromArray(F, dst, i);
            VectorMask<Float> keep = v.compare(VectorOperators.LT, value)
                                      .or(v.compare(VectorOperators.EQ, value)
                                           .and(v.reinterpretAsInts()
                                                 .compare(VectorOperators.LT, valueBits)
                                                 .cast(F)));
            broadcastValue.blend(v, keep).intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] = Float.compare(dst[i], value) < 0 ? dst[i] : value;
        }
    }

    @Override
    public void subtractMultiplyAdd(float[] dst, float subtrahend, float multiplier, float addend, int len) {
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            // Deliberately sub -> mul -> add as separate roundings; FMA would change results
            FloatVector.fromArray(F, dst, i).sub(subtrahend).mul(multiplier).add(addend).intoArray(dst, i);
        }
        for (; i < len; i++) {
            dst[i] = (dst[i] - subtrahend) * multiplier + addend;
        }
    }

    @Override
    public float min(float[] array, int len) {
        // Math.min is associative and commutative (NaN-propagating, -0.0 < +0.0), so lane order
        // and vector width cannot affect the result.
        FloatVector acc = FloatVector.broadcast(F, Float.POSITIVE_INFINITY);
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            acc = acc.min(FloatVector.fromArray(F, array, i));
        }
        float min = acc.reduceLanes(VectorOperators.MIN);
        for (; i < len; i++) {
            min = Math.min(min, array[i]);
        }
        return min;
    }

    @Override
    public float max(float[] array, int len) {
        FloatVector acc = FloatVector.broadcast(F, Float.NEGATIVE_INFINITY);
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            acc = acc.max(FloatVector.fromArray(F, array, i));
        }
        float max = acc.reduceLanes(VectorOperators.MAX);
        for (; i < len; i++) {
            max = Math.max(max, array[i]);
        }
        return max;
    }

    @Override
    public void roundScaled(float[] src, int srcOffset, int[] dst, int dstOffset, float factor, int len) {
        // Math.round is floor(x + 0.5) in EXACT arithmetic (not float addition of 0.5, which
        // double-rounds for values like 0.49999997f). floor(x) + (x - floor(x) >= 0.5 ? 1 : 0)
        // computes exactly that: x - floor(x) is exact by Sterbenz's lemma. The Vector API has no
        // lanewise floor, so floor is synthesized from the truncating F2I conversion (exact: every
        // float with |x| >= 2^24 is already integral, and F2I's saturation/NaN semantics combined
        // with the correction below reproduce Math.round at the extremes).
        int i = 0;
        int bound = F.loopBound(len);
        for (; i < bound; i += F.length()) {
            FloatVector scaled = FloatVector.fromArray(F, src, srcOffset + i).mul(factor);
            FloatVector truncated = (FloatVector) scaled.convert(F2I, 0).convert(I2F, 0);
            FloatVector floor = truncated.sub(1f, truncated.compare(VectorOperators.GT, scaled));
            VectorMask<Float> roundUp = scaled.sub(floor).compare(VectorOperators.GE, 0.5f);
            IntVector rounded = (IntVector) floor.add(1f, roundUp).convert(F2I, 0);
            rounded.intoArray(dst, dstOffset + i);
        }
        for (; i < len; i++) {
            dst[dstOffset + i] = StrictMath.round(src[srcOffset + i] * factor);
        }
    }

    @Override
    public void satBoxBlurRow(int[] sat, int aOffset, int bOffset, int cOffset, int dOffset, float area,
                              float postDivisor, float[] dst, int dstOffset, int len) {
        int i = 0;
        int bound = I.loopBound(len);
        for (; i < bound; i += I.length()) {
            IntVector count = IntVector.fromArray(I, sat, dOffset + i)
                                       .add(IntVector.fromArray(I, sat, aOffset + i))
                                       .sub(IntVector.fromArray(I, sat, bOffset + i))
                                       .sub(IntVector.fromArray(I, sat, cOffset + i));
            FloatVector value = (FloatVector) count.convert(I2F, 0);
            value.div(area).div(postDivisor).intoArray(dst, dstOffset + i);
        }
        for (; i < len; i++) {
            int count = sat[dOffset + i] + sat[aOffset + i] - sat[bOffset + i] - sat[cOffset + i];
            dst[dstOffset + i] = ((float) count / area) / postDivisor;
        }
    }
}
