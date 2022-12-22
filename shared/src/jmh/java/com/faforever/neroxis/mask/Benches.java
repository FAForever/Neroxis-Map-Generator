package com.faforever.neroxis.mask;


import com.faforever.neroxis.util.vector.Vector3;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;

@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 2, time = 5)
@Fork(value = 2)
@BenchmarkMode(Mode.Throughput)
public class Benches {

    private static final int ITER = 100;

    @Benchmark
    public void D1(Blackhole blackhole) {
        Vector3 v1 = new Vector3();
        Vector3 v2 = new Vector3();

        v1.randomize(new Random(), 1f);
        v2.randomize(new Random(), 1f);
        for (int i = 0; i < ITER; i++) {
            v1.add(v2);
        }

        blackhole.consume(v1);
    }

    @Benchmark
    public void D2(Blackhole blackhole) {
        float[] v1 = new float[3];
        float[] v2 = new float[3];

        Random r1 = new Random();
        for (int i = 0; i < v1.length; ++i) {
            v1[i] = r1.nextFloat() * 1;
        }

        Random r2 = new Random();
        for (int i = 0; i < v2.length; ++i) {
            v2[i] = r2.nextFloat() * 1;
        }

        for (int j = 0; j < ITER; j++) {
            for (int i = 0; i < v1.length; ++i) {
                v1[i] += v2[i];
            }
        }

        blackhole.consume(v1);
    }

}
