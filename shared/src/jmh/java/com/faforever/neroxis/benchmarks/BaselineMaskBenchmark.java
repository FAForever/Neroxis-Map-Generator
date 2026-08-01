package com.faforever.neroxis.benchmarks;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
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
 * Baseline timings for the CPU-hot {@link FloatMask} operations, measured through the public mask
 * API exactly as the generator uses them. Recorded before any optimization work so later changes
 * can be compared against a committed reference (docs/benchmarks/).
 *
 * <p>Masks are rebuilt every iteration so in-place mutation cannot drift values into
 * denormals/infinities and distort timings. Noise ranges are chosen so repeated multiply/add stay
 * well inside the normal float range within one measurement iteration.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class BaselineMaskBenchmark {
    @Param({"256", "512", "1024"})
    int size;

    FloatMask base;
    FloatMask nearOne;
    BooleanMask booleanMask;

    @Setup(Level.Iteration)
    public void setup() {
        SymmetrySettings none = new SymmetrySettings(Symmetry.NONE);
        base = new FloatMask(size, new SplittableRandom(1234), none, "base").addWhiteNoise(0f, 1f);
        nearOne = new FloatMask(size, new SplittableRandom(5678), none, "nearOne").addWhiteNoise(0.999f, 1.001f);
        booleanMask = new BooleanMask(size, new SplittableRandom(9876), none, "bool").randomize(0.3f);
    }

    @Benchmark
    public FloatMask addMask() {
        return base.add(nearOne);
    }

    @Benchmark
    public FloatMask subtractMask() {
        return base.subtract(nearOne);
    }

    @Benchmark
    public FloatMask multiplyMask() {
        return base.multiply(nearOne);
    }

    @Benchmark
    public FloatMask addScalar() {
        return base.add(0.001f);
    }

    @Benchmark
    public FloatMask multiplyScalar() {
        return base.multiply(1.0001f);
    }

    @Benchmark
    public FloatMask sqrt() {
        return nearOne.sqrt();
    }

    @Benchmark
    public FloatMask clampMin() {
        return base.clampMin(0.25f);
    }

    @Benchmark
    public FloatMask scaleToNewMinAndMaxHeight() {
        return base.scaleToNewMinAndMaxHeight(0f, 100f);
    }

    @Benchmark
    public FloatMask blurRadius1() {
        return base.blur(1);
    }

    @Benchmark
    public FloatMask blurRadius4() {
        return base.blur(4);
    }

    @Benchmark
    public FloatMask blurRadius16() {
        return base.blur(16);
    }

    @Benchmark
    public FloatMask addPerlinNoise() {
        return base.addPerlinNoise(8, 0.1f);
    }

    @Benchmark
    public float getMin() {
        return base.getMin();
    }

    @Benchmark
    public float getMax() {
        return base.getMax();
    }

    @Benchmark
    public float getSum() {
        return base.getSum();
    }

    @Benchmark
    public FloatMask distanceField() {
        return booleanMask.copyAsDistanceField();
    }
}
