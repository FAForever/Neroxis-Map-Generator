# Neroxis Map Generator for Supreme Commander Forged Alliance

Procedurally generate Supreme Commander Maps from a seed to enable new gameplay on maps never before seen.

### How to Generate Maps

Download the .jar file included in the github release and ensure you have at least Java 8 installed on your machine.
Once downloaded run the following command in your terminal to generate a map in the current directory

`java -jar NeroxisGen_X.X.X.jar`

Default settings will generate a 6 player 10km map. Generator options can be viewed by running

`java -jar NeroxisGen_X.X.X.jar --help`

### Performance / SIMD

The hot mask math has a SIMD implementation built on the JDK Vector API (`jdk.incubator.vector`)
alongside the default scalar loops. Both paths are bit-identical, so the same seed produces the
same map either way.

The SIMD path is **opt-in** (`-Dneroxis.vector.enabled=true`, plus
`--add-modules jdk.incubator.vector` when running the jar from the classpath) because the
generator normally runs one map per JVM and Vector API code is slow until the JIT compiles it —
for a single map that warmup costs more than SIMD saves, even with the release images' AOT cache.
Enable it for long-lived processes that generate many maps.

Benchmarks:

- JMH micro-benchmarks: `./gradlew :shared:jmh` (filter with `-PjmhIncludes=<regex>`,
  restrict sizes with `-PjmhSizes=256,512`). Results land in `shared/build/reports/jmh/results.json`.
- End-to-end wall clock: `scripts/benchmark-e2e.sh path/to/NeroxisGen.jar` (requires
  [hyperfine](https://github.com/sharkdp/hyperfine)).
- Determinism check (scalar vs vector output must be bit-identical):
  `scripts/check-determinism.sh path/to/NeroxisGen.jar` — also runs in CI.

### Development

Project built using gradle and developed with the Intellij IDE.