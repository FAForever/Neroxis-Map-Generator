package com.faforever.neroxis.generator.util;

import com.faforever.neroxis.generator.ElementGenerator;
import com.faforever.neroxis.generator.GeneratorOptions;
import com.faforever.neroxis.generator.GeneratorParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GeneratorSelector {

    public static <T extends ElementGenerator> T selectRandomMatchingGenerator(Random random, GeneratorOptions<T> generatorOptions,
                                                                               GeneratorParameters generatorParameters) {
        if (generatorOptions.possibleGenerators().isEmpty()) {
            return generatorOptions.fallbackGenerator();
        }

        List<T> matchingGenerators = generatorOptions.possibleGenerators().stream()
                                                     .filter(generator -> generator.getParameterConstraints()
                                                                                   .matches(generatorParameters))
                                                     .collect(Collectors.toList());
        return selectRandomGeneratorUsingWeights(random, matchingGenerators, generatorOptions.fallbackGenerator());
    }

    private static <T extends ElementGenerator> T selectRandomGeneratorUsingWeights(Random random, List<T> generators,
                                                                                    T defaultGenerator) {
        if (!generators.isEmpty()) {
            List<Float> weights = generators.stream().map(ElementGenerator::getWeight).toList();
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
                                    .map(weight -> generators.get(cumulativeWeights.indexOf(weight)))
                                    .orElse(defaultGenerator);
        } else {
            return defaultGenerator;
        }
    }

}
