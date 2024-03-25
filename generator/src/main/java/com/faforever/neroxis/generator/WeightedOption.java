package com.faforever.neroxis.generator;

import java.util.Objects;

public record WeightedOption<T>(
        T option,
        float weight
) {
    public WeightedOption {
        Objects.requireNonNull(option, "Option cannot be null");
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be greater than 0");
        }
    }
}
