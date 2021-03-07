package neroxis.generator.mapstyles;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
    DEFAULT(DefaultStyleGenerator.class, Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 16), Arrays.asList(256, 512, 1024), Range.of(0, 16), Range.of(0, 1000), 1),
    ONE_ISLAND(OneIslandStyleGenerator.class, Range.of(0f, .5f), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 16), Arrays.asList(512, 1024), Range.of(0, 16), Range.of(0, 1000), 1),
    BIG_ISLANDS(BigIslandsStyleGenerator.class, Range.of(0f, .75f), Range.of(0, 1), Range.of(0, .5f), Range.of(0, 1), Range.of(0, 1), Range.of(.5f, 1), Range.of(0, 16), Arrays.asList(1024), Range.of(0, 16), Range.of(0, 1000), 1),
    SMALL_ISLANDS(SmallIslandsStyleGenerator.class, Range.of(0f, .5f), Range.of(0, 1), Range.of(0, .25f), Range.of(0, 1), Range.of(0, 1), Range.of(.5f, 1), Range.of(0, 16), Arrays.asList(1024), Range.of(0, 16), Range.of(0, 1000), 1),
    CENTER_LAKE(CenterLakeStyleGenerator.class, Range.of(0f, .5f), Range.of(0, 1), Range.of(0, .5f), Range.of(0, 1), Range.of(0, 1), Range.of(.25f, 1), Range.of(0, 16), Arrays.asList(512, 1024), Range.of(0, 16), Range.of(0, 1000), 1),
    VALLEY(ValleyStyleGenerator.class, Range.of(.75f, 1f), Range.of(.5f, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 16), Arrays.asList(512, 1024), Range.of(0, 16), Range.of(0, 1000), 1),
    LITTLE_MOUNTAIN(LittleMountainStyleGenerator.class, Range.of(.5f, 1f), Range.of(.5f, 1), Range.of(0, .5f), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 16), Arrays.asList(256, 512, 1024), Range.of(0, 16), Range.of(0, 1000), 1),
    MOUNTAIN_RANGE(MountainRangeStyleGenerator.class, Range.of(.5f, 1f), Range.of(.5f, 1), Range.of(0, .5f), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 16), Arrays.asList(256, 512), Range.of(0, 16), Range.of(0, 1000), 1);

    private final Class<? extends DefaultStyleGenerator> generatorClass;
    private final Range landDensityRange;
    private final Range mountainDensityRange;
    private final Range plateauDensityRange;
    private final Range rampDensityRange;
    private final Range reclaimDensityRange;
    private final Range mexDensityRange;
    private final Range spawnCountRange;
    private final List<Integer> mapSizes;
    private final Range numTeamsRange;
    private final Range hydroCountRange;
    private final float weight;

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

    public SCMap generate(MapParameters mapParameters, Random random) throws Exception {
        return generatorClass.getDeclaredConstructor(MapParameters.class, Random.class)
                .newInstance(mapParameters, random)
                .generate();
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

}
