package neroxis.generator;

import lombok.Value;
import neroxis.biomes.Biome;
import neroxis.map.MapParameters;
import neroxis.map.SymmetrySettings;
import neroxis.util.Range;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Value
public strictfp class ParameterConstraints {
    Range landDensityRange;
    Range mountainDensityRange;
    Range plateauDensityRange;
    Range rampDensityRange;
    Range reclaimDensityRange;
    Range mexDensityRange;
    Range spawnCountRange;
    List<Integer> mapSizes;
    Range numTeamsRange;
    Range hydroCountRange;

    public static ParameterConstraintsBuilder builder() {
        return new ParameterConstraintsBuilder();
    }

    public boolean matches(MapParameters mapParameters) {
        return landDensityRange.contains(mapParameters.getLandDensity())
                && mountainDensityRange.contains(mapParameters.getMountainDensity())
                && plateauDensityRange.contains(mapParameters.getPlateauDensity())
                && rampDensityRange.contains(mapParameters.getRampDensity())
                && reclaimDensityRange.contains(mapParameters.getReclaimDensity())
                && mexDensityRange.contains(mapParameters.getMexDensity())
                && hydroCountRange.contains(mapParameters.getHydroCount())
                && numTeamsRange.contains(mapParameters.getNumTeams())
                && spawnCountRange.contains(mapParameters.getSpawnCount())
                && mapSizes.contains(mapParameters.getMapSize());
    }

    public MapParameters initParameters(Random random, int spawnCount, int mapSize, int numTeams, Biome biome, SymmetrySettings symmetrySettings) {
        return MapParameters.builder()
                .spawnCount(spawnCount)
                .landDensity(landDensityRange.getRandomFloat(random))
                .plateauDensity(plateauDensityRange.getRandomFloat(random))
                .mountainDensity(mountainDensityRange.getRandomFloat(random))
                .rampDensity(rampDensityRange.getRandomFloat(random))
                .reclaimDensity(reclaimDensityRange.getRandomFloat(random))
                .mexDensity(mexDensityRange.getRandomFloat(random))
                .mapSize(mapSize)
                .numTeams(numTeams)
                .hydroCount(spawnCount)
                .unexplored(false)
                .symmetrySettings(symmetrySettings)
                .biome(biome)
                .build();
    }

    public static class ParameterConstraintsBuilder {
        Range landDensityRange = Range.of(0, 1);
        Range mountainDensityRange = Range.of(0, 1);
        Range plateauDensityRange = Range.of(0, 1);
        Range rampDensityRange = Range.of(0, 1);
        Range reclaimDensityRange = Range.of(0, 1);
        Range mexDensityRange = Range.of(0, 1);
        Range spawnCountRange = Range.of(0, 16);
        List<Integer> mapSizes = Arrays.asList(256, 512, 1024);
        Range numTeamsRange = Range.of(0, 16);
        Range hydroCountRange = Range.of(0, 32);

        public ParameterConstraints build() {
            return new ParameterConstraints(landDensityRange, mountainDensityRange, plateauDensityRange,
                    rampDensityRange, reclaimDensityRange, mexDensityRange, spawnCountRange, mapSizes,
                    numTeamsRange, hydroCountRange);
        }

        public ParameterConstraintsBuilder landDensity(float min, float max) {
            landDensityRange = Range.of(min, max);
            return this;
        }

        public ParameterConstraintsBuilder mountainDensity(float min, float max) {
            mountainDensityRange = Range.of(min, max);
            return this;
        }

        public ParameterConstraintsBuilder plateauDensity(float min, float max) {
            plateauDensityRange = Range.of(min, max);
            return this;
        }

        public ParameterConstraintsBuilder rampDensity(float min, float max) {
            rampDensityRange = Range.of(min, max);
            return this;
        }

        public ParameterConstraintsBuilder reclaimDensity(float min, float max) {
            reclaimDensityRange = Range.of(min, max);
            return this;
        }

        public ParameterConstraintsBuilder mexDensity(float min, float max) {
            mexDensityRange = Range.of(min, max);
            return this;
        }

        public ParameterConstraintsBuilder spawnCount(float min, float max) {
            spawnCountRange = Range.of(min, max);
            return this;
        }

        public ParameterConstraintsBuilder mapSizes(Integer... sizes) {
            mapSizes = Arrays.asList(sizes);
            return this;
        }

        public ParameterConstraintsBuilder numTeams(float min, float max) {
            numTeamsRange = Range.of(min, max);
            return this;
        }

        public ParameterConstraintsBuilder hydroCount(float min, float max) {
            hydroCountRange = Range.of(min, max);
            return this;
        }
    }
}
