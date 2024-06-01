package com.faforever.neroxis.generator;

import com.faforever.neroxis.util.Range;

public record ParameterConstraints(Range spawnCountRange,
                                   Range mapSizeRange,
                                   Range numTeamsRange) {
    public static ParameterConstraintsBuilder builder() {
        return new ParameterConstraintsBuilder();
    }

    public boolean matches(GeneratorParameters generatorParameters) {
        return numTeamsRange.contains(generatorParameters.numTeams())
               && spawnCountRange.contains(generatorParameters.spawnCount())
               && mapSizeRange.contains(generatorParameters.mapSize());
    }

    public static class ParameterConstraintsBuilder {
        Range spawnCountRange = Range.of(0, 16);
        Range mapSizeRange = Range.of(0, 2048);
        Range numTeamsRange = Range.of(0, 16);

        public ParameterConstraints build() {
            return new ParameterConstraints(spawnCountRange,
                                            mapSizeRange, numTeamsRange);
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
    }
}
