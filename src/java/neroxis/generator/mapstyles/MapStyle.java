package neroxis.generator.mapstyles;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import neroxis.biomes.Biome;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;
import neroxis.map.SymmetrySettings;
import neroxis.util.Range;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RequiredArgsConstructor
@Getter
public strictfp enum MapStyle {
    DEFAULT(DefaultStyleGenerator.class, StyleConstraints.builder().build(), 1),
    ONE_ISLAND(OneIslandStyleGenerator.class, StyleConstraints.builder().landDensity(0f, .75f).mapSizes(512, 1024).build(), 2),
    BIG_ISLANDS(BigIslandsStyleGenerator.class, StyleConstraints.builder().landDensity(0f, .75f).plateauDensity(0, .5f).mapSizes(1024).build(), 2),
    SMALL_ISLANDS(SmallIslandsStyleGenerator.class, StyleConstraints.builder().landDensity(0f, .5f).plateauDensity(0, .5f).mexDensity(.5f, 1).mapSizes(1024).build(), 2),
    CENTER_LAKE(CenterLakeStyleGenerator.class, StyleConstraints.builder().landDensity(0f, .5f).plateauDensity(0, .5f).mexDensity(.25f, 1).mapSizes(512, 1024).build(), 1),
    VALLEY(ValleyStyleGenerator.class, StyleConstraints.builder().landDensity(.75f, 1f).mountainDensity(.5f, 1).mapSizes(512, 1024).build(), 2),
    DROP_PLATEAU(DropPlateauStyleGenerator.class, StyleConstraints.builder().landDensity(.5f, 1f).plateauDensity(.5f, 1).mexDensity(.25f, 1).build(), 2),
    LITTLE_MOUNTAIN(LittleMountainStyleGenerator.class, StyleConstraints.builder().landDensity(.5f, 1f).mountainDensity(.25f, 1).plateauDensity(0, .5f).build(), 2),
    MOUNTAIN_RANGE(MountainRangeStyleGenerator.class, StyleConstraints.builder().landDensity(.5f, 1f).mountainDensity(.5f, 1).plateauDensity(0, .5f).mapSizes(256, 512).build(), 2);

    private final Class<? extends DefaultStyleGenerator> generatorClass;
    private final StyleConstraints styleConstraints;
    private final float weight;

    public boolean matches(MapParameters mapParameters) {
        return styleConstraints.landDensityRange.contains(mapParameters.getLandDensity())
                && styleConstraints.mountainDensityRange.contains(mapParameters.getMountainDensity())
                && styleConstraints.plateauDensityRange.contains(mapParameters.getPlateauDensity())
                && styleConstraints.rampDensityRange.contains(mapParameters.getRampDensity())
                && styleConstraints.reclaimDensityRange.contains(mapParameters.getReclaimDensity())
                && styleConstraints.mexDensityRange.contains(mapParameters.getMexDensity())
                && styleConstraints.hydroCountRange.contains(mapParameters.getHydroCount())
                && styleConstraints.numTeamsRange.contains(mapParameters.getNumTeams())
                && styleConstraints.spawnCountRange.contains(mapParameters.getSpawnCount())
                && styleConstraints.mapSizes.contains(mapParameters.getMapSize());
    }

    public SCMap generate(MapParameters mapParameters, Random random) throws Exception {
        return generatorClass.getDeclaredConstructor(MapParameters.class, Random.class)
                .newInstance(mapParameters, random)
                .generate();
    }

    public MapParameters initParameters(Random random, int spawnCount, int mapSize, int numTeams, Biome biome, SymmetrySettings symmetrySettings) {
        return MapParameters.builder()
                .spawnCount(spawnCount)
                .landDensity(styleConstraints.landDensityRange.getRandomFloat(random))
                .plateauDensity(styleConstraints.plateauDensityRange.getRandomFloat(random))
                .mountainDensity(styleConstraints.mountainDensityRange.getRandomFloat(random))
                .rampDensity(styleConstraints.rampDensityRange.getRandomFloat(random))
                .reclaimDensity(styleConstraints.reclaimDensityRange.getRandomFloat(random))
                .mexDensity(styleConstraints.mexDensityRange.getRandomFloat(random))
                .mapSize(mapSize)
                .numTeams(numTeams)
                .hydroCount(spawnCount)
                .unexplored(false)
                .symmetrySettings(symmetrySettings)
                .biome(biome)
                .build();
    }

    @Value
    public static strictfp class StyleConstraints {
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

        public static StyleConstraintsBuilder builder() {
            return new StyleConstraintsBuilder();
        }

        private static class StyleConstraintsBuilder {
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

            public StyleConstraints build() {
                return new StyleConstraints(landDensityRange, mountainDensityRange, plateauDensityRange,
                        rampDensityRange, reclaimDensityRange, mexDensityRange, spawnCountRange, mapSizes,
                        numTeamsRange, hydroCountRange);
            }

            public StyleConstraintsBuilder landDensity(float min, float max) {
                landDensityRange = Range.of(min, max);
                return this;
            }

            public StyleConstraintsBuilder mountainDensity(float min, float max) {
                mountainDensityRange = Range.of(min, max);
                return this;
            }

            public StyleConstraintsBuilder plateauDensity(float min, float max) {
                plateauDensityRange = Range.of(min, max);
                return this;
            }

            public StyleConstraintsBuilder rampDensity(float min, float max) {
                rampDensityRange = Range.of(min, max);
                return this;
            }

            public StyleConstraintsBuilder reclaimDensity(float min, float max) {
                reclaimDensityRange = Range.of(min, max);
                return this;
            }

            public StyleConstraintsBuilder mexDensity(float min, float max) {
                landDensityRange = Range.of(min, max);
                return this;
            }

            public StyleConstraintsBuilder spawnCount(float min, float max) {
                spawnCountRange = Range.of(min, max);
                return this;
            }

            public StyleConstraintsBuilder mapSizes(Integer... sizes) {
                mapSizes = Arrays.asList(sizes);
                return this;
            }

            public StyleConstraintsBuilder numTeams(float min, float max) {
                numTeamsRange = Range.of(min, max);
                return this;
            }

            public StyleConstraintsBuilder hydroCount(float min, float max) {
                hydroCountRange = Range.of(min, max);
                return this;
            }
        }
    }
}
