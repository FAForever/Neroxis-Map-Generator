package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.util.HasParameterConstraints;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

public sealed interface WeightedOptionsWithFallback<T> {

    @SafeVarargs
    static <T extends HasParameterConstraints> WeightedOptionsWithFallback<T> of(T fallback, WeightedOption<T>... options) {
        if (options.length == 0) {
            return new WeightedOptionsWithFallback.Single<>(fallback);
        }

        return new WeightedOptionsWithFallback.Multi<>(fallback, List.of(options));
    }

    default T select(Random random) {
        return select(random, option -> true);
    }

    default T select(Random random, Predicate<? super T> filter) {
        return switch (this) {
            case Single(T option) -> option;
            case Multi(T fallbackOption, List<WeightedOption<T>> options) -> {
                List<WeightedOption<T>> matchingOptions = options.stream()
                                                                 .filter(weightedOption -> filter.test(
                                                                         weightedOption.option()))
                                                                 .toList();
                if (matchingOptions.isEmpty()) {
                    yield fallbackOption;
                }

                double[] weights = matchingOptions.stream().mapToDouble(WeightedOption::weight).toArray();
                double[] cumulativeWeights = new double[weights.length];
                double sum = 0;
                for (int i = 0; i < weights.length; i++) {
                    sum += weights[i];
                    cumulativeWeights[i] = sum;
                }
                double value = random.nextDouble(sum);
                int index = Arrays.binarySearch(cumulativeWeights, value);
                if (index < 0) {
                    index = (index + 1) * -1;
                }
                yield matchingOptions.get(index).option();
            }
        };
    }

    record Multi<T>(
            T fallbackOption,
            List<WeightedOption<T>> options
    ) implements WeightedOptionsWithFallback<T> {

        public Multi {
            Objects.requireNonNull(fallbackOption, "Fallback cannot be null");
            if (options.size() < 2) {
                throw new IllegalArgumentException("At least two options must be provided");
            }
            options = List.copyOf(options);
        }

    }

    record Single<T>(T option) implements WeightedOptionsWithFallback<T> {
        public Single {
            Objects.requireNonNull(option, "Option cannot be null");
        }
    }
}
