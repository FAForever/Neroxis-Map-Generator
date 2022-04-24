package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.util.Range;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import lombok.Value;

@SuppressWarnings({"unused"})
@Value
public strictfp class ParameterConstraints {

    Range landDensityRange;
    Range mountainDensityRange;
    Range plateauDensityRange;
    Range rampDensityRange;
    Range reclaimDensityRange;
    Range mexDensityRange;
    Range spawnCountRange;
    Range mapSizeRange;
    Range numTeamsRange;
    List<String> biomes;

    public static ParameterConstraintsBuilder builder() {
        return new ParameterConstraintsBuilder();
    }

    public boolean matches(GeneratorParameters generatorParameters) {
        return landDensityRange.contains(generatorParameters.getLandDensity())
               && mountainDensityRange.contains(generatorParameters.getMountainDensity())
               && plateauDensityRange.contains(generatorParameters.getPlateauDensity())
               && rampDensityRange.contains(generatorParameters.getRampDensity())
               && reclaimDensityRange.contains(generatorParameters.getReclaimDensity())
               && mexDensityRange.contains(generatorParameters.getMexDensity())
               && numTeamsRange.contains(generatorParameters.getNumTeams())
               && spawnCountRange.contains(generatorParameters.getSpawnCount())
               && mapSizeRange.contains(generatorParameters.getMapSize())
               && biomes.contains(generatorParameters.getBiome().getName());
    }

    public boolean matches(int mapSize, int numTeams, int spawnCount) {
        return mapSizeRange.contains(mapSize) && numTeamsRange.contains(numTeams) && spawnCountRange.contains(
                spawnCount);
    }

    public GeneratorParameters randomizeParameters(Random random, GeneratorParameters generatorParameters) {
        return GeneratorParameters.builder()
                                  .spawnCount(generatorParameters.getSpawnCount())
                                  .landDensity(landDensityRange.getRandomFloat(random))
                                  .plateauDensity(plateauDensityRange.getRandomFloat(random))
                                  .mountainDensity(mountainDensityRange.getRandomFloat(random))
                                  .rampDensity(rampDensityRange.getRandomFloat(random))
                                  .reclaimDensity(reclaimDensityRange.getRandomFloat(random))
                                  .mexDensity(mexDensityRange.getRandomFloat(random))
                                  .mapSize(generatorParameters.getMapSize())
                                  .numTeams(generatorParameters.getNumTeams())
                                  .visibility(generatorParameters.getVisibility())
                                  .terrainSymmetry(generatorParameters.getTerrainSymmetry())
                                  .biome(Biomes.loadBiome(biomes.get(random.nextInt(biomes.size()))))
                                  .build();
    }

    public GeneratorParameters mapToLevel(float level, GeneratorParameters generatorParameters, Random random) {
        return GeneratorParameters.builder()
                                  .spawnCount(generatorParameters.getSpawnCount())
                                  .landDensity(landDensityRange.map(level))
                                  .plateauDensity(plateauDensityRange.map(level))
                                  .mountainDensity(mountainDensityRange.map(level))
                                  .rampDensity(rampDensityRange.map(level))
                                  .reclaimDensity(reclaimDensityRange.map(level))
                                  .mexDensity(mexDensityRange.map(level))
                                  .mapSize(generatorParameters.getMapSize())
                                  .numTeams(generatorParameters.getNumTeams())
                                  .visibility(generatorParameters.getVisibility())
                                  .terrainSymmetry(generatorParameters.getTerrainSymmetry())
                                  .biome(Biomes.loadBiome(biomes.get(random.nextInt(biomes.size()))))
                                  .build();
    }

    public GeneratorParameters initParameters(Random random,
                                              GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        return generatorParametersBuilder.landDensity(landDensityRange.getRandomFloat(random))
                                         .plateauDensity(plateauDensityRange.getRandomFloat(random))
                                         .mountainDensity(mountainDensityRange.getRandomFloat(random))
                                         .rampDensity(rampDensityRange.getRandomFloat(random))
                                         .reclaimDensity(reclaimDensityRange.getRandomFloat(random))
                                         .mexDensity(mexDensityRange.getRandomFloat(random))
                                         .biome(Biomes.loadBiome(biomes.get(random.nextInt(biomes.size()))))
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
        Range mapSizeRange = Range.of(0, 2048);
        Range numTeamsRange = Range.of(0, 16);
        Range hydroCountRange = Range.of(0, 32);
        List<String> biomes = new ArrayList<>(Biomes.BIOMES_LIST);

        public ParameterConstraints build() {
            return new ParameterConstraints(landDensityRange, mountainDensityRange, plateauDensityRange,
                                            rampDensityRange, reclaimDensityRange, mexDensityRange, spawnCountRange,
                                            mapSizeRange, numTeamsRange, biomes);
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

        public ParameterConstraintsBuilder mapSizes(float min, float max) {
            mapSizeRange = Range.of(min, max);
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

        public ParameterConstraintsBuilder biomes(String... biomeNames) {
            biomes = Arrays.asList(biomeNames);
            return this;
        }
    }
}
