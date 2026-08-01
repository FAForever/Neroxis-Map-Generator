package com.faforever.neroxis.util.ops;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verifies that {@link VectorFloatArrayOps} is bit-identical to {@link ScalarFloatArrayOps} for
 * every operation, including NaN, infinities, signed zeros and denormals. Bit identity between the
 * two implementations is what keeps map generation deterministic across machines regardless of
 * whether the Vector API module is available (see {@link FloatArrayOps}).
 */
class FloatArrayOpsEquivalenceTest {
    // Odd length exercises the scalar tail after the species loop for every supported lane count
    private static final int LENGTH = 1027;
    private static final float[] EDGE_VALUES = {
            0.0f, -0.0f, 1.0f, -1.0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
            Float.MIN_VALUE, -Float.MIN_VALUE, Float.MIN_NORMAL, -Float.MIN_NORMAL,
            Float.MAX_VALUE, -Float.MAX_VALUE, 1e-38f, -1e-38f, 1234.5678f, -1234.5678f
    };
    private static final float[] CLAMP_VALUES = {
            0.0f, -0.0f, 1.0f, -1.0f, 0.25f, -255f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
            Float.MIN_VALUE, Float.MAX_VALUE
    };

    private static final ScalarFloatArrayOps SCALAR = new ScalarFloatArrayOps();
    private static FloatArrayOps vector;

    @BeforeAll
    static void loadVectorImplementation() {
        assumeTrue(ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent(),
                   "jdk.incubator.vector not resolved; vector equivalence not testable in this JVM");
        vector = new VectorFloatArrayOps();
    }

    private static float[] testData() {
        SplittableRandom random = new SplittableRandom(1234);
        float[] data = new float[LENGTH];
        for (int i = 0; i < data.length; i++) {
            data[i] = (random.nextFloat() - 0.5f) * 1000f;
        }
        // Sprinkle edge values throughout, including inside vector lanes and the tail
        for (int i = 0; i < EDGE_VALUES.length; i++) {
            data[i * 7 % data.length] = EDGE_VALUES[i];
            data[data.length - 1 - i] = EDGE_VALUES[i];
        }
        return data;
    }

    private static void assertBitIdentical(BiConsumer<FloatArrayOps, float[]> operation, String description) {
        float[] scalarData = testData();
        float[] vectorData = testData();
        operation.accept(SCALAR, scalarData);
        operation.accept(vector, vectorData);
        for (int i = 0; i < LENGTH; i++) {
            assertEquals(Float.floatToRawIntBits(scalarData[i]), Float.floatToRawIntBits(vectorData[i]),
                         description + " differs at index " + i + ": scalar=" + scalarData[i] + " vector="
                         + vectorData[i]);
        }
    }

    @Test
    void arrayOperandOpsAreBitIdentical() {
        float[] src = testData();
        assertBitIdentical((ops, dst) -> ops.add(dst, src, LENGTH), "add(array)");
        assertBitIdentical((ops, dst) -> ops.subtract(dst, src, LENGTH), "subtract(array)");
        assertBitIdentical((ops, dst) -> ops.multiply(dst, src, LENGTH), "multiply(array)");
        assertBitIdentical((ops, dst) -> ops.divide(dst, src, LENGTH), "divide(array)");
    }

    @Test
    void scalarOperandOpsAreBitIdentical() {
        for (float value : EDGE_VALUES) {
            assertBitIdentical((ops, dst) -> ops.add(dst, value, LENGTH), "add(" + value + ")");
            assertBitIdentical((ops, dst) -> ops.subtract(dst, value, LENGTH), "subtract(" + value + ")");
            assertBitIdentical((ops, dst) -> ops.multiply(dst, value, LENGTH), "multiply(" + value + ")");
            assertBitIdentical((ops, dst) -> ops.divide(dst, value, LENGTH), "divide(" + value + ")");
        }
    }

    @Test
    void sqrtIsBitIdentical() {
        assertBitIdentical((ops, dst) -> ops.sqrt(dst, LENGTH), "sqrt");
    }

    @Test
    void clampsAreBitIdentical() {
        for (float value : CLAMP_VALUES) {
            assertBitIdentical((ops, dst) -> ops.clampMin(dst, value, LENGTH), "clampMin(" + value + ")");
            assertBitIdentical((ops, dst) -> ops.clampMax(dst, value, LENGTH), "clampMax(" + value + ")");
        }
    }

    @Test
    void subtractMultiplyAddIsBitIdentical() {
        assertBitIdentical((ops, dst) -> ops.subtractMultiplyAdd(dst, 12.34f, 0.7531f, -45.6f, LENGTH),
                           "subtractMultiplyAdd");
        assertBitIdentical((ops, dst) -> ops.subtractMultiplyAdd(dst, -0.0f, 1e30f, Float.MIN_VALUE, LENGTH),
                           "subtractMultiplyAdd(extremes)");
    }

    @Test
    void reductionsAreBitIdentical() {
        float[] data = testData();
        assertEquals(Float.floatToRawIntBits(SCALAR.min(data, LENGTH)),
                     Float.floatToRawIntBits(vector.min(data, LENGTH)), "min");
        assertEquals(Float.floatToRawIntBits(SCALAR.max(data, LENGTH)),
                     Float.floatToRawIntBits(vector.max(data, LENGTH)), "max");

        // Without NaN (NaN is absorbing and hides ordering bugs) and with signed-zero ties
        SplittableRandom random = new SplittableRandom(99);
        float[] finiteData = new float[LENGTH];
        for (int i = 0; i < finiteData.length; i++) {
            finiteData[i] = (random.nextFloat() - 0.5f) * 2000f;
        }
        finiteData[3] = -0.0f;
        finiteData[LENGTH - 2] = 0.0f;
        assertEquals(Float.floatToRawIntBits(SCALAR.min(finiteData, LENGTH)),
                     Float.floatToRawIntBits(vector.min(finiteData, LENGTH)), "min(finite)");
        assertEquals(Float.floatToRawIntBits(SCALAR.max(finiteData, LENGTH)),
                     Float.floatToRawIntBits(vector.max(finiteData, LENGTH)), "max(finite)");
    }

    @Test
    void partialLengthLeavesTailUntouched() {
        int partialLength = 100;
        float[] scalarData = testData();
        float[] vectorData = scalarData.clone();
        float[] original = scalarData.clone();
        SCALAR.multiply(scalarData, 3.5f, partialLength);
        vector.multiply(vectorData, 3.5f, partialLength);
        for (int i = 0; i < LENGTH; i++) {
            assertEquals(Float.floatToRawIntBits(scalarData[i]), Float.floatToRawIntBits(vectorData[i]),
                         "partial multiply differs at index " + i);
        }
        for (int i = partialLength; i < LENGTH; i++) {
            assertEquals(Float.floatToRawIntBits(original[i]), Float.floatToRawIntBits(vectorData[i]),
                         "element beyond len modified at index " + i);
        }
    }
}
