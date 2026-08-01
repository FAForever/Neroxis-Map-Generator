package com.faforever.neroxis.util.ops;

/**
 * Selects the {@link FloatArrayOps} implementation once at class initialization.
 *
 * <p>The SIMD implementation is <b>opt-in</b> ({@code -Dneroxis.vector.enabled=true}) and
 * additionally requires the incubator module {@code jdk.incubator.vector} to be resolved — it is
 * on the module path (jlink/jpackage images) automatically, while classpath/shadow-jar launches
 * need {@code --add-modules jdk.incubator.vector} (executable-jar manifests cannot enable it).
 * Both implementations are bit-identical, so the choice never affects generated map content.
 *
 * <p>Why opt-in: the generator normally runs one map per JVM, and Vector API code executes in a
 * slow fallback until C2 compiles it. Measured on a single 512 map, that warmup costs more than
 * SIMD saves — even with the release images' AOT cache. The vector path pays off only for
 * long-lived processes generating many maps; revisit if the Vector API graduates from incubation
 * or AOT starts covering its intrinsics.
 */
public final class FloatArrayOpsHolder {
    public static final FloatArrayOps OPS;
    /** Whether the SIMD implementation is active, for diagnostics/debug logging. */
    public static final boolean VECTORIZED;

    static {
        FloatArrayOps ops = resolve();
        OPS = ops;
        VECTORIZED = !(ops instanceof ScalarFloatArrayOps);
    }

    private FloatArrayOpsHolder() {
    }

    private static FloatArrayOps resolve() {
        if (!Boolean.parseBoolean(System.getProperty("neroxis.vector.enabled", "false"))) {
            return new ScalarFloatArrayOps();
        }
        if (ModuleLayer.boot().findModule("jdk.incubator.vector").isEmpty()) {
            return new ScalarFloatArrayOps();
        }
        try {
            // Reflective load keeps this class free of any static reference to the Vector API, so
            // resolution failures on exotic setups can never escape as linkage errors.
            return (FloatArrayOps) Class.forName("com.faforever.neroxis.util.ops.VectorFloatArrayOps")
                                        .getDeclaredConstructor()
                                        .newInstance();
        } catch (Throwable t) {
            return new ScalarFloatArrayOps();
        }
    }
}
