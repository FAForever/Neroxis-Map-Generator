package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.map.SymmetrySettings;

import java.util.Random;

public class GraphContext {
    public enum SupplierType {
        SEED, SYMMETRY_SETTINGS, USER_SPECIFIED;
    }

    private final Random random;
    private final SymmetrySettings symmetrySettings;

    public GraphContext(long seed, SymmetrySettings symmetrySettings) {
        random = new Random(seed);
        this.symmetrySettings = symmetrySettings;
    }

    public Object getSuppliedValue(SupplierType supplierType) {
        return switch (supplierType) {
            case SEED -> random.nextLong();
            case SYMMETRY_SETTINGS -> symmetrySettings;
            default -> throw new IllegalStateException("Unexpected value: " + supplierType);
        };
    }
}
