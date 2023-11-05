package com.faforever.neroxis.generator;

import java.util.List;
import java.util.Objects;

public record GeneratorOptions<T extends ElementGenerator>(T fallbackGenerator, List<T> possibleGenerators) {

    public GeneratorOptions(T fallbackGenerator) {
        this(fallbackGenerator, List.of());
    }

    public GeneratorOptions {
        Objects.requireNonNull(fallbackGenerator, "Fallback cannot be null");
        possibleGenerators = List.copyOf(possibleGenerators);
    }
}
