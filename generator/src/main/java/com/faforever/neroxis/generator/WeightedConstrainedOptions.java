package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.util.HasParameterConstraints;

import java.util.List;
import java.util.Objects;

public record WeightedConstrainedOptions<T extends HasParameterConstraints>(T fallbackOption,
                                                                            List<WeightedOption<T>> options) {

    @SafeVarargs
    public WeightedConstrainedOptions(T fallbackOption, WeightedOption<T>... options) {
        this(fallbackOption, List.of(options));
    }

    public WeightedConstrainedOptions {
        Objects.requireNonNull(fallbackOption, "Fallback cannot be null");
        options = List.copyOf(options);
    }

    public static <T extends HasParameterConstraints> WeightedConstrainedOptions<T> single(T option) {
        return new WeightedConstrainedOptions<>(option, List.of());
    }
}
