package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.exporter.PreviewGenerator;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.ImageUtil;
import com.faforever.neroxis.util.MathUtil;
import com.faforever.neroxis.util.Pipeline;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import picocli.CommandLine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.faforever.neroxis.util.ImageUtil.compareImages;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
public class MapGeneratorTest {
    String mapName = "neroxis_map_generator_snapshot_aaaaaaaaaacne_aicaebsicfsuqek5cm";
    long seed = 1234;
    byte spawnCount = 2;
    float landDensity = .56746f;
    float plateauDensity = .1324f;
    float mountainDensity = .7956f;
    float rampDensity = .5649f;
    float reclaimDensity = .1354f;
    float mexDensity = .7325f;
    int mapSize = 256;
    int numTeams = 2;
    float roundedLandDensity = MathUtil.discretePercentage(landDensity, 127);
    float roundedPlateauDensity = MathUtil.discretePercentage(plateauDensity, 127);
    float roundedMountainDensity = MathUtil.discretePercentage(mountainDensity, 127);
    float roundedRampDensity = MathUtil.discretePercentage(rampDensity, 127);
    float roundedReclaimDensity = MathUtil.discretePercentage(reclaimDensity, 127);
    float roundedMexDensity = MathUtil.discretePercentage(mexDensity, 127);
    String[] keywordArgs;
    private MapGenerator instance;

    @BeforeEach
    public void setup() {
        keywordArgs = new String[]{"--seed", Long.toString(seed), "--spawn-count", Byte.toString(spawnCount),
                                   "--land-density", Float.toString(landDensity), "--plateau-density",
                                   Float.toString(plateauDensity), "--mountain-density",
                                   Float.toString(mountainDensity), "--ramp-density", Float.toString(rampDensity),
                                   "--reclaim-density", Float.toString(reclaimDensity), "--mex-density",
                                   Float.toString(mexDensity), "--map-size", Integer.toString(mapSize), "--num-teams",
                                   Integer.toString(numTeams)};

        instance = new MapGenerator();
    }

    @Test
    public void TestParseMapName() {
        new CommandLine(instance).execute("--map-name", mapName);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getOutputFolderMixin().getOutputPath(), Path.of("."));
        GeneratorParameters generatorParameters = instance.getGeneratorParameters();
        assertEquals(generatorParameters.landDensity(), roundedLandDensity);
        assertEquals(generatorParameters.plateauDensity(), roundedPlateauDensity);
        assertEquals(generatorParameters.mountainDensity(), roundedMountainDensity);
        assertEquals(generatorParameters.rampDensity(), roundedRampDensity);
        assertEquals(generatorParameters.reclaimDensity(), roundedReclaimDensity);
        assertEquals(generatorParameters.mexDensity(), roundedMexDensity);
        assertEquals(generatorParameters.mapSize(), mapSize);
    }

    @ParameterizedTest
    @ArgumentsSource(ValidMapSizeArgumentProvider.class)
    @Disabled("Flaky Test, OOM")
    public void TestMapExportedToProperSize(int mapSize) {
        new CommandLine(instance).execute("--map-size", String.valueOf(mapSize));

        SCMap map = instance.getMap();

        assertEquals(0, (StrictMath.log(map.getSize()) / StrictMath.log(2)) % 1);
    }

    @Test
    public void TestDeterminism() {
        new CommandLine(instance).execute(keywordArgs);
        assertEquals(instance.getGeneratorParameters(), instance.getStyleGenerator().getGeneratorParameters());

        SCMap map1 = instance.getMap();
        String[] hashArray1 = Pipeline.getHashArray().clone();

        instance = new MapGenerator();

        new CommandLine(instance).execute(keywordArgs);
        assertEquals(instance.getGeneratorParameters(), instance.getStyleGenerator().getGeneratorParameters());

        SCMap map2 = instance.getMap();
        String[] hashArray2 = Pipeline.getHashArray().clone();

        assertSCMapEquality(map1, map2);
        assertArrayEquals(hashArray1, hashArray2);
    }

    @Test
    public void TestMultipleGenerationDeterminism() {
        instance = new MapGenerator();
        new CommandLine(instance).execute("--num-to-generate", "2", "--map-size", "256");
        assertEquals(instance.getGeneratorParameters(), instance.getStyleGenerator().getGeneratorParameters());
        SCMap map1 = instance.getMap();
        String[] hashArray1 = Pipeline.getHashArray().clone();

        instance = new MapGenerator();
        new CommandLine(instance).execute("--map-name", map1.getName());
        assertEquals(instance.getGeneratorParameters(), instance.getStyleGenerator().getGeneratorParameters());
        SCMap map2 = instance.getMap();
        String[] hashArray2 = Pipeline.getHashArray().clone();

        assertArrayEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestEqualityMapNameKeyword() {
        new CommandLine(instance).execute(keywordArgs);
        SCMap map1 = instance.getMap();

        instance = new MapGenerator();

        String[] args = {"--map-name", map1.getName()};
        new CommandLine(instance).execute(args);
        SCMap map2 = instance.getMap();

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestEqualityWithDebugMapNameKeyword() {
        new CommandLine(instance).execute(keywordArgs);
        SCMap map1 = instance.getMap();

        instance = new MapGenerator();

        String[] args = {"--map-name", map1.getName(), "--debug"};
        new CommandLine(instance).execute(args);
        SCMap map2 = instance.getMap();

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestEqualityTournamentStyle() {
        new CommandLine(instance).execute("--tournament-style", "--map-size", "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestInequalityTournamentStyle() throws Exception {
        new CommandLine(instance).execute("--tournament-style", "--map-size", "256");
        SCMap map1 = instance.getMap();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getSeed();

        Thread.sleep(1000);
        instance = new MapGenerator();

        new CommandLine(instance).execute("--tournament-style", "--seed", String.valueOf(seed1), "--map-size", "256");
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getSeed();

        assertNotEquals(map1.getName(), map2.getName());
        assertNotEquals(generationTime1, generationTime2);
        assertNotEquals(seed1, seed2);
        assertNotEquals(map1.getSpawns(), map2.getSpawns());
        assertNotEquals(map1.getMexes(), map2.getMexes());
        assertNotEquals(map1.getHydros(), map2.getHydros());
        assertNotEquals(map1.getProps(), map2.getProps());
        assertEquals(map1.getSize(), map2.getSize());
        assertFalse(compareImages(map1.getPreview(), map2.getPreview()));
        assertFalse(compareImages(map1.getHeightmap(), map2.getHeightmap()));
        assertFalse(compareImages(map1.getTextureMasksHigh(), map2.getTextureMasksHigh()));
        assertFalse(compareImages(map1.getTextureMasksLow(), map2.getTextureMasksLow()));
    }

    @Test
    public void TestEqualityBlind() {
        new CommandLine(instance).execute("--blind", "--map-size", "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestEqualityUnexplored() {
        new CommandLine(instance).execute("--unexplored", "--map-size", "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(MapStyleArgumentProvider.class)
    public void TestEqualityStyleSpecified(MapStyle style) {
        instance = new MapGenerator();

        new CommandLine(instance).execute("--style", style.toString(), "--map-size", "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(BiomeArgumentProvider.class)
    public void TestEqualityBiomeSpecified(String biome) {
        instance = new MapGenerator();

        new CommandLine(instance).execute("--biome", biome, "--map-size", "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestUnexploredNoUnits() {
        for (int i = 0; i < 10; ++i) {
            instance = new MapGenerator();
            new CommandLine(instance).execute("--unexplored", "--map-size", "256");
            SCMap map = instance.getMap();

            for (Army army : map.getArmies()) {
                for (Group group : army.getGroups()) {
                    assertEquals(0, group.getUnits().size());
                }
            }
        }
    }

    @Test
    public void TestUnexploredPreview() throws Exception {
        instance = new MapGenerator();
        new CommandLine(instance).execute("--unexplored", "--map-size", "256");
        SCMap map = instance.getMap();

        BufferedImage blankPreview = ImageUtil.readImage(PreviewGenerator.BLANK_PREVIEW);
        BufferedImage mapPreview = map.getPreview();

        assertArrayEquals(blankPreview.getRGB(0, 0, 256, 256, null, 0, 256),
                          mapPreview.getRGB(0, 0, 256, 256, null, 0, 256));
    }

    @AfterEach
    public void cleanup() throws IOException {
        DebugUtil.DEBUG = false;
        try (Stream<Path> list = Files.list(Path.of("."))) {
            list.filter(path -> path.getFileName().toString().startsWith("neroxis_map_generator_snapshot"))
                .forEach(FileUtil::deleteRecursiveIfExists);
        }
    }

    private static class BiomeArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Biomes.BIOMES_LIST.stream().map(Arguments::of);
        }
    }

    private static class MapStyleArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(MapStyle.values()).map(Arguments::of);
        }
    }

    private static class ValidMapSizeArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return IntStream.iterate(128, size -> size < 2048, size -> size + 64).mapToObj(Arguments::of);
        }
    }

    private void assertSCMapEquality(SCMap map1, SCMap map2) {
        assertEquals(map1.getName(), map2.getName());
        assertEquals(map1.getSpawns(), map2.getSpawns());
        assertEquals(map1.getMexes(), map2.getMexes());
        assertEquals(map1.getHydros(), map2.getHydros());
        assertEquals(map1.getArmies(), map2.getArmies());
        assertEquals(map1.getProps(), map2.getProps());
        assertEquals(map1.getBiome(), map2.getBiome());
        assertEquals(map1.getSize(), map2.getSize());
        assertTrue(compareImages(map1.getPreview(), map2.getPreview()));
        assertTrue(compareImages(map1.getHeightmap(), map2.getHeightmap()));
        assertTrue(compareImages(map1.getNormalMap(), map2.getNormalMap()));
        assertTrue(compareImages(map1.getTextureMasksHigh(), map2.getTextureMasksHigh()));
        assertTrue(compareImages(map1.getTextureMasksLow(), map2.getTextureMasksLow()));
        assertTrue(compareImages(map1.getWaterMap(), map2.getWaterMap()));
        assertTrue(compareImages(map1.getWaterFoamMap(), map2.getWaterFoamMap()));
        assertTrue(compareImages(map1.getWaterDepthBiasMap(), map2.getWaterDepthBiasMap()));
        assertTrue(compareImages(map1.getWaterShadowMap(), map2.getWaterShadowMap()));
        assertTrue(compareImages(map1.getTerrainType(), map2.getTerrainType()));
    }
}


