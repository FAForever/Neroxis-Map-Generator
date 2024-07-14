package com.faforever.neroxis.mask;


import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 2, time = 5)
@Fork(value = 2)
@BenchmarkMode(Mode.Throughput)
public class Benches {

    private final BooleanMask booleanMask = new BooleanMask(128, null, new SymmetrySettings(Symmetry.NONE));

    @Benchmark
    public void directLoop(Blackhole blackhole) {
        booleanMask.loopWithSymmetry(SymmetryType.SPAWN, (x, y) -> {
//            blackhole.consume(x);
//            blackhole.consume(y);
        });
    }

}
