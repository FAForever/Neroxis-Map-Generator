package com.faforever.neroxis.generator.util;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.WeightedConstrainedOptions;
import com.faforever.neroxis.generator.WeightedOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ConstrainedSelector {

    public static <T extends HasParameterConstraints> T selectRandomMatchingOption(Random random, WeightedConstrainedOptions<T> generatorOptions, GeneratorParameters generatorParameters) {
        if (generatorOptions.options().isEmpty()) {
            return generatorOptions.fallbackOption();
        }

        List<WeightedOption<T>> matchingGenerators = generatorOptions.options()
                                                                     .stream()
                                                                     .filter(weightedOption -> weightedOption.option()
                                                                                                             .getParameterConstraints()
                                                                                                             .matches(
                                                                                                                     generatorParameters))
                                                                     .collect(Collectors.toList());
        return selectRandomOptionUsingWeights(random, matchingGenerators, generatorOptions.fallbackOption());
    }

    private static <T> T selectRandomOptionUsingWeights(Random random, List<WeightedOption<T>> weightedOptions, T defaultGenerator) {
        if (!weightedOptions.isEmpty()) {
            List<Float> weights = weightedOptions.stream().map(WeightedOption::weight).toList();
            List<Float> cumulativeWeights = new ArrayList<>();
            float sum = 0;
            for (float weight : weights) {
                sum += weight;
                cumulativeWeights.add(sum);
            }
            float value = random.nextFloat() * cumulativeWeights.get(cumulativeWeights.size() - 1);
            return cumulativeWeights.stream()
                                    .filter(weight -> value <= weight)
                                    .findFirst()
                                    .map(weight -> weightedOptions.get(cumulativeWeights.indexOf(weight)))
                                    .map(WeightedOption::option)
                                    .orElse(defaultGenerator);
        } else {
            return defaultGenerator;
        }
    }

}
