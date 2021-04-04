package neroxis.util;

import neroxis.generator.ElementGenerator;
import neroxis.map.MapParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public strictfp class RandomUtils {

    public static boolean andRandomBoolean(Random random, int numToAnd) {
        for (int i = 0; i < numToAnd; i++) {
            if (!random.nextBoolean()) {
                return false;
            }
        }
        return true;
    }

    public static float sumRandomFloat(Random random, int numToSum) {
        float value = 0;
        for (int i = 0; i < numToSum; i++) {
            value += random.nextFloat();
        }
        return value;
    }

    public static float sumRandomInt(Random random, int numToSum) {
        float value = 0;
        for (int i = 0; i < numToSum; i++) {
            value += random.nextFloat();
        }
        return value;
    }

    public static float averageRandomFloat(Random random, int numToAvg) {
        return sumRandomFloat(random, numToAvg) / numToAvg;
    }

    public static float averageRandomInt(Random random, int numToAvg) {
        return sumRandomInt(random, numToAvg) / numToAvg;
    }

    public static <T extends ElementGenerator> T selectRandomMatchingGenerator(Random random, List<T> generators,
                                                                               MapParameters mapParameters, T defaultGenerator) {
        List<T> matchingGenerators = generators.stream()
                .filter(generator -> generator.getParameterConstraints().matches(mapParameters))
                .collect(Collectors.toList());
        if (matchingGenerators.size() > 0) {
            List<Float> weights = matchingGenerators.stream().map(ElementGenerator::getWeight).collect(Collectors.toList());
            List<Float> cumulativeWeights = new ArrayList<>();
            float sum = 0;
            for (float weight : weights) {
                sum += weight;
                cumulativeWeights.add(sum);
            }
            float value = random.nextFloat() * cumulativeWeights.get(cumulativeWeights.size() - 1);
            return cumulativeWeights.stream().filter(weight -> value <= weight)
                    .reduce((first, second) -> first)
                    .map(weight -> matchingGenerators.get(cumulativeWeights.indexOf(weight)))
                    .orElse(defaultGenerator);
        } else {
            return defaultGenerator;
        }
    }

}
