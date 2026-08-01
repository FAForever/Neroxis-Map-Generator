package com.faforever.neroxis.benchmarks;

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

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Isolates the cost of the boxed DoubleStream reductions currently used by
 * FloatMask.getMin/getMax/getSum against plain primitive loops over the same data, both in the
 * existing jagged float[x][y] layout and a flat float[] layout. This quantifies how much of a
 * reduction speedup comes from dropping streams versus SIMD.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class ReductionBenchmark {
    @Param({"256", "512", "1024"})
    int size;

    float[][] jagged;
    float[] flat;

    @Setup(Level.Trial)
    public void setup() {
        SplittableRandom random = new SplittableRandom(1234);
        jagged = new float[size][size];
        flat = new float[size * size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                float value = random.nextFloat();
                jagged[x][y] = value;
                flat[x * size + y] = value;
            }
        }
    }

    // Mirrors FloatMask.getSum()
    @Benchmark
    public float sumStreamJagged() {
        return (float) Arrays.stream(jagged)
                             .flatMapToDouble(row -> IntStream.range(0, row.length).mapToDouble(i -> row[i]))
                             .sum();
    }

    // Mirrors FloatMask.getMin()
    @Benchmark
    public float minStreamJagged() {
        return (float) Arrays.stream(jagged)
                             .flatMapToDouble(row -> IntStream.range(0, row.length).mapToDouble(i -> row[i]))
                             .min()
                             .orElseThrow();
    }

    @Benchmark
    public float sumLoopJagged() {
        double sum = 0;
        for (float[] row : jagged) {
            for (float value : row) {
                sum += value;
            }
        }
        return (float) sum;
    }

    @Benchmark
    public float minLoopJagged() {
        float min = Float.POSITIVE_INFINITY;
        for (float[] row : jagged) {
            for (float value : row) {
                min = Math.min(min, value);
            }
        }
        return min;
    }

    @Benchmark
    public float sumLoopFlat() {
        double sum = 0;
        for (float value : flat) {
            sum += value;
        }
        return (float) sum;
    }

    @Benchmark
    public float minLoopFlat() {
        float min = Float.POSITIVE_INFINITY;
        for (float value : flat) {
            min = Math.min(min, value);
        }
        return min;
    }
}
