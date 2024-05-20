package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.util.Range;

import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

public record ParameterConstraints(Range spawnCountRange,
                                   Range mapSizeRange,
                                   Range numTeamsRange,
                                   List<BiomeName> biomes) {
    public static ParameterConstraintsBuilder builder() {
        return new ParameterConstraintsBuilder();
    }

    public boolean matches(GeneratorParameters generatorParameters) {
        return numTeamsRange.contains(generatorParameters.numTeams())
               && spawnCountRange.contains(generatorParameters.spawnCount())
               && mapSizeRange.contains(generatorParameters.mapSize())
               && biomes.contains(generatorParameters.biome().name());
    }

    public GeneratorParameters.GeneratorParametersBuilder chooseBiome(RandomGenerator random,
                                                                      GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        return generatorParametersBuilder.biome(Biomes.loadBiome(biomes.get(random.nextInt(biomes.size()))));
    }

    public static class ParameterConstraintsBuilder {
        Range spawnCountRange = Range.of(0, 16);
        Range mapSizeRange = Range.of(0, 2048);
        Range numTeamsRange = Range.of(0, 16);
        List<BiomeName> biomes = Arrays.stream(BiomeName.values()).toList();

        public ParameterConstraints build() {
            return new ParameterConstraints(spawnCountRange,
                                            mapSizeRange, numTeamsRange, biomes);
        }

        public ParameterConstraintsBuilder spawnCount(float min, float max) {
            spawnCountRange = Range.of(min, max);
            return this;
        }

        public ParameterConstraintsBuilder mapSizes(float min, float max) {
            mapSizeRange = Range.of(min, max);
            return this;
        }

        public ParameterConstraintsBuilder numTeams(float min, float max) {
            numTeamsRange = Range.of(min, max);
            return this;
        }

        public ParameterConstraintsBuilder biomes(BiomeName... biomeNames) {
            biomes = List.of(biomeNames);
            return this;
        }
    }
}
