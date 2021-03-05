package neroxis.generator.mapstyles;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import neroxis.biomes.Biome;
import neroxis.generator.MapGenerator;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;
import neroxis.map.SymmetrySettings;
import neroxis.util.RandomUtils;
import neroxis.util.Range;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RequiredArgsConstructor
public enum MapStyle {
    DEFAULT(DefaultStyleGenerator.class, Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 16), Arrays.asList(256, 512, 1024), Range.of(0, 16), Range.of(0, 1000), Range.of(0, 1000), 1),
    BIG_ISLAND(BigIslandStyleGenerator.class, Range.of(0f, .5f), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 16), Arrays.asList(512, 1024), Range.of(0, 16), Range.of(0, 1000), Range.of(0, 1000), 1),
    BIG_LAKE(BigLakeStyleGenerator.class, Range.of(0f, .5f), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 16), Arrays.asList(512, 1024), Range.of(0, 16), Range.of(0, 1000), Range.of(0, 1000), 1),
    VALLEY(ValleyStyleGenerator.class, Range.of(.75f, 1f), Range.of(.5f, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 1), Range.of(0, 16), Arrays.asList(512, 1024), Range.of(0, 16), Range.of(0, 1000), Range.of(0, 1000), 1);

    private final Class<? extends BaseStyleGenerator> generatorClass;
    private final Range landDensityRange;
    private final Range mountainDensityRange;
    private final Range plateauDensityRange;
    private final Range rampDensityRange;
    private final Range reclaimDensityRange;
    private final Range spawnCountRange;
    private final List<Integer> mapSizes;
    private final Range numTeamsRange;
    private final Range mexCountRange;
    private final Range hydroCountRange;
    @Getter
    private final int weight;
    @Getter
    @Setter
    private float probability;

    public boolean matches(MapParameters mapParameters) {
        return landDensityRange.contains(mapParameters.getLandDensity())
                && mountainDensityRange.contains(mapParameters.getMountainDensity())
                && plateauDensityRange.contains(mapParameters.getPlateauDensity())
                && rampDensityRange.contains(mapParameters.getRampDensity())
                && reclaimDensityRange.contains(mapParameters.getReclaimDensity())
                && mexCountRange.contains(mapParameters.getMexCount())
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

    public MapParameters initParameters(Random random, int spawnCount, int mapSize, Biome biome) {
        float landDensity = landDensityRange.getRandomFloat(random);
        float mountainDensity = mountainDensityRange.getRandomFloat(random);
        float plateauDensity = plateauDensityRange.getRandomFloat(random);
        float rampDensity = rampDensityRange.getRandomFloat(random);
        float reclaimDensity = reclaimDensityRange.getRandomFloat(random);
        int mexCount = MapGenerator.getMexCount(RandomUtils.averageRandomFloat(random, 2), spawnCount, mapSize);
        int numTeams = 2;
        int hydroCount = spawnCount;
        SymmetrySettings symmetrySettings = MapGenerator.initSymmetrySettings(MapGenerator.getValidSymmetry(spawnCount, numTeams, random), spawnCount, numTeams, random);
        return new MapParameters(spawnCount, landDensity, plateauDensity, mountainDensity, rampDensity, reclaimDensity, mapSize, numTeams, mexCount, hydroCount, false, symmetrySettings, biome);
    }

}
