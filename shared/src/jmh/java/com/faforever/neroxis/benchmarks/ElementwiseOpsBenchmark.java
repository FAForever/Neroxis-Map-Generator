package com.faforever.neroxis.benchmarks;

import com.faforever.neroxis.util.ops.FloatArrayOps;
import com.faforever.neroxis.util.ops.ScalarFloatArrayOps;
import com.faforever.neroxis.util.ops.VectorFloatArrayOps;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

/**
 * Direct scalar-vs-vector comparison of the {@link FloatArrayOps} implementations on flat arrays
 * sized like real masks. Complements {@link BaselineMaskBenchmark}, which measures through the
 * mask API.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class ElementwiseOpsBenchmark {
    @Param({"256", "512", "1024"})
    int size;
    @Param({"scalar", "vector"})
    String impl;

    FloatArrayOps ops;
    float[] dst;
    float[] src;

    @Setup(Level.Trial)
    public void setupTrial() {
        ops = switch (impl) {
            case "scalar" -> new ScalarFloatArrayOps();
            case "vector" -> new VectorFloatArrayOps();
            default -> throw new IllegalArgumentException(impl);
        };
        src = new float[size * size];
        dst = new float[size * size];
        SplittableRandom random = new SplittableRandom(1234);
        for (int i = 0; i < src.length; i++) {
            src[i] = random.nextFloat() * 0.002f + 0.999f;
        }
    }

    @Setup(Level.Iteration)
    public void resetDst() {
        SplittableRandom random = new SplittableRandom(5678);
        for (int i = 0; i < dst.length; i++) {
            dst[i] = random.nextFloat();
        }
    }

    @Benchmark
    public float[] addArray() {
        ops.add(dst, src, dst.length);
        return dst;
    }

    @Benchmark
    public float[] multiplyArray() {
        ops.multiply(dst, src, dst.length);
        return dst;
    }

    @Benchmark
    public float[] addScalar() {
        ops.add(dst, 0.001f, dst.length);
        return dst;
    }

    @Benchmark
    public float[] sqrt() {
        ops.sqrt(dst, dst.length);
        return dst;
    }

    @Benchmark
    public float[] clampMin() {
        ops.clampMin(dst, 0.25f, dst.length);
        return dst;
    }

    @Benchmark
    public float[] subtractMultiplyAdd() {
        ops.subtractMultiplyAdd(dst, 0.1f, 1.8f, 0.05f, dst.length);
        return dst;
    }

    @Benchmark
    public float minReduction() {
        return ops.min(dst, dst.length);
    }

    @Benchmark
    public float maxReduction() {
        return ops.max(dst, dst.length);
    }
}
