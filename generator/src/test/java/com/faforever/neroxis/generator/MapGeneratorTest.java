package com.faforever.neroxis.generator;

import com.faforever.neroxis.exporter.PreviewGenerator;
import com.faforever.neroxis.generator.cli.CustomStyleOptions;
import com.faforever.neroxis.generator.style.CustomStyleGenerator;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.ImageUtil;
import com.faforever.neroxis.util.MathUtil;
import com.faforever.neroxis.util.Pipeline;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
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
import java.util.Random;
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
    public static final int NUM_DETERMINISM_REPEATS = 3;
    String mapName = "neroxis_map_generator_snapshot_aaaaaaaaaacne_aicaedyaaeaqeek5";
    long seed = 1234;
    byte spawnCount = 2;
    TerrainStyle terrainStyle = TerrainStyle.BIG_ISLANDS;
    TextureStyle textureStyle = TextureStyle.BRIMSTONE;
    ResourceStyle resourceStyle = ResourceStyle.LOW_MEX;
    PropStyle propStyle = PropStyle.ENEMY_CIV;
    float reclaimDensity = .1354f;
    float resourceDensity = .7325f;
    float roundedReclaimDensity = MathUtil.discretePercentage(reclaimDensity, 127);
    float roundedResourceDensity = MathUtil.discretePercentage(resourceDensity, 127);
    Symmetry symmetry = Symmetry.XZ;
    int mapSize = 256;
    int numTeams = 2;
    String[] keywordArgs;
    private MapGenerator instance;

    @BeforeEach
    public void setup() {
        keywordArgs = new String[]{"--seed", Long.toString(seed), "--spawn-count", Byte.toString(spawnCount),
                                   "--terrain-style", terrainStyle.name(), "--texture-style", textureStyle.name(),
                                   "--resource-style", resourceStyle.name(), "--prop-style", propStyle.name(),
                                   "--terrain-symmetry", symmetry.name(), "--map-size", Integer.toString(mapSize),
                                   "--resource-density", Float.toString(resourceDensity), "--reclaim-density",
                                   Float.toString(reclaimDensity),
                                   "--num-teams", Integer.toString(numTeams)};

        instance = new MapGenerator();
    }

    @Test
    public void TestParseMapName() {
        new CommandLine(instance).execute("--map-name", mapName);

        assertEquals(instance.getBasicOptions().getSeed(), seed);
        assertEquals(instance.getOutputFolderMixin().getOutputPath(), Path.of("."));
        GeneratorParameters generatorParameters = instance.getGeneratorParameters();
        CustomStyleOptions customStyleOptions = instance.getGenerationOptions()
                                                        .getCasualOptions()
                                                        .getStyleOptions()
                                                        .getCustomStyleOptions();

        assertEquals(CustomStyleGenerator.class, instance.getStyleGenerator().getClass());
        assertEquals(customStyleOptions.getTerrainStyle(), terrainStyle);
        assertEquals(customStyleOptions.getTextureStyle(), textureStyle);
        assertEquals(customStyleOptions.getResourceStyle(), resourceStyle);
        assertEquals(customStyleOptions.getPropStyle(), propStyle);
        assertEquals(customStyleOptions.getReclaimDensity(), roundedReclaimDensity);
        assertEquals(customStyleOptions.getResourceDensity(), roundedResourceDensity);
        assertEquals(generatorParameters.terrainSymmetry(), symmetry);
        assertEquals(generatorParameters.numTeams(), numTeams);
        assertEquals(generatorParameters.mapSize(), mapSize);
    }

    @ParameterizedTest
    @ArgumentsSource(ValidMapSizeArgumentProvider.class)
    public void TestMapExportedToProperSize(int mapSize) {
        new CommandLine(instance).execute("--map-size", String.valueOf(mapSize));

        SCMap map = instance.getMap();

        assertEquals(0, (StrictMath.log(map.getSize()) / StrictMath.log(2)) % 1);
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
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

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityWithDebugMapNameKeyword() {
        new CommandLine(instance).execute(keywordArgs);
        SCMap map1 = instance.getMap();

        instance = new MapGenerator();

        String[] args = {"--map-name", map1.getName(), "--debug"};
        new CommandLine(instance).execute(args);
        SCMap map2 = instance.getMap();

        assertSCMapEquality(map1, map2);
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityTournamentStyle() {
        new CommandLine(instance).execute("--tournament-style", "--map-size", "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestInequalityTournamentStyle() throws Exception {
        new CommandLine(instance).execute("--tournament-style", "--map-size", "256");
        SCMap map1 = instance.getMap();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        Thread.sleep(1000);
        instance = new MapGenerator();

        new CommandLine(instance).execute("--tournament-style", "--seed", String.valueOf(seed1), "--map-size", "256");
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

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

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityBlind() {
        new CommandLine(instance).execute("--blind", "--map-size", "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestInequalityBlind() throws Exception {
        new CommandLine(instance).execute("--blind", "--map-size", "256");
        SCMap map1 = instance.getMap();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        Thread.sleep(1000);
        instance = new MapGenerator();

        new CommandLine(instance).execute("--blind", "--seed", String.valueOf(seed1), "--map-size", "256");
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertNotEquals(map1.getName(), map2.getName());
        assertNotEquals(generationTime1, generationTime2);
        assertNotEquals(seed1, seed2);
        assertNotEquals(map1.getSpawns(), map2.getSpawns());
        assertNotEquals(map1.getMexes(), map2.getMexes());
        assertNotEquals(map1.getHydros(), map2.getHydros());
        assertNotEquals(map1.getProps(), map2.getProps());
        assertEquals(map1.getSize(), map2.getSize());
        assertTrue(compareImages(map1.getPreview(), map2.getPreview()));
        assertFalse(compareImages(map1.getHeightmap(), map2.getHeightmap()));
        assertFalse(compareImages(map1.getTextureMasksHigh(), map2.getTextureMasksHigh()));
        assertFalse(compareImages(map1.getTextureMasksLow(), map2.getTextureMasksLow()));
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityUnexplored() {
        new CommandLine(instance).execute("--unexplored", "--map-size", "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestInequalityUnexplored() throws Exception {
        new CommandLine(instance).execute("--unexplored", "--map-size", "256");
        SCMap map1 = instance.getMap();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        Thread.sleep(1000);
        instance = new MapGenerator();

        new CommandLine(instance).execute("--unexplored", "--seed", String.valueOf(seed1), "--map-size", "256");
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertNotEquals(map1.getName(), map2.getName());
        assertNotEquals(generationTime1, generationTime2);
        assertNotEquals(seed1, seed2);
        assertNotEquals(map1.getSpawns(), map2.getSpawns());
        assertNotEquals(map1.getMexes(), map2.getMexes());
        assertNotEquals(map1.getHydros(), map2.getHydros());
        assertNotEquals(map1.getProps(), map2.getProps());
        assertEquals(map1.getSize(), map2.getSize());
        assertTrue(compareImages(map1.getPreview(), map2.getPreview()));
        assertFalse(compareImages(map1.getHeightmap(), map2.getHeightmap()));
        assertFalse(compareImages(map1.getTextureMasksHigh(), map2.getTextureMasksHigh()));
        assertFalse(compareImages(map1.getTextureMasksLow(), map2.getTextureMasksLow()));
    }

    @ParameterizedTest
    @ArgumentsSource(MapStyleArgumentProvider.class)
    public void TestEqualityStyleSpecified(MapStyle style) {
        instance = new MapGenerator();

        new CommandLine(instance).execute("--style", style.toString(), "--map-size", "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(SymmetryArgumentProvider.class)
    public void TestEqualitySymmetrySpecified(Symmetry symmetry) {
        instance = new MapGenerator();

        int numTeams = switch (symmetry) {
            case Symmetry s when s.getNumSymPoints() == 1 -> 0;
            case Symmetry s when s.getNumSymPoints() % 2 == 0 -> 2;
            case Symmetry s when s.getNumSymPoints() % 3 == 0 -> 3;
            case Symmetry s -> s.getNumSymPoints();
        };

        int spawnCount = numTeams == 0 ? 4 : numTeams;

        new CommandLine(instance).execute("--terrain-symmetry", symmetry.toString(), "--map-size", "256", "--num-teams",
                                          String.valueOf(numTeams), "--spawn-count", String.valueOf(spawnCount)
                                         );
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(TerrainGeneratorArgumentProvider.class)
    public void TestEqualityTerrainGeneratorSpecified(TerrainStyle terrainStyle) {
        instance = new MapGenerator();

        new CommandLine(instance).execute("--terrain-style", terrainStyle.toString(), "--map-size",
                                          "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(TextureGeneratorArgumentProvider.class)
    public void TestEqualityTextureGeneratorSpecified(TextureStyle textureStyle) {
        instance = new MapGenerator();

        new CommandLine(instance).execute("--texture-style", textureStyle.toString(), "--map-size",
                                          "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(ResourceGeneratorArgumentProvider.class)
    public void TestEqualityResourceGeneratorSpecified(ResourceStyle resourceStyle) {
        instance = new MapGenerator();

        new CommandLine(instance).execute("--resource-style", resourceStyle.toString(), "--map-size",
                                          "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(PropGeneratorArgumentProvider.class)
    public void TestEqualityPropGeneratorSpecified(PropStyle propStyle) {
        instance = new MapGenerator();

        new CommandLine(instance).execute("--prop-style", propStyle.toString(), "--map-size",
                                          "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityReclaimDensitySpecified() {
        instance = new MapGenerator();

        new CommandLine(instance).execute("--reclaim-density", String.valueOf(new Random().nextFloat()), "--map-size",
                                          "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityResourceDensitySpecified() {
        instance = new MapGenerator();

        new CommandLine(instance).execute("--resource-density", String.valueOf(new Random().nextFloat()), "--map-size",
                                          "256");
        SCMap map1 = instance.getMap();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getBasicOptions().getSeed();

        instance = new MapGenerator();

        new CommandLine(instance).execute("--map-name", mapName);
        SCMap map2 = instance.getMap();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @RepeatedTest(10)
    public void TestUnexploredNoUnits() {
        instance = new MapGenerator();
        new CommandLine(instance).execute("--unexplored", "--map-size", "256");
        SCMap map = instance.getMap();

        for (Army army : map.getArmies()) {
            for (Group group : army.getGroups()) {
                assertEquals(0, group.getUnits().size());
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

    private static class SymmetryArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(Symmetry.values()).mapMulti(((symmetry, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(symmetry);
                }
            })).map(Arguments::of);
        }
    }

    private static class MapStyleArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(MapStyle.values()).mapMulti(((mapStyle, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(mapStyle);
                }
            })).map(Arguments::of);
        }
    }

    private static class TerrainGeneratorArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(TerrainStyle.values()).mapMulti(((generator, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(generator);
                }
            })).map(Arguments::of);
        }
    }

    private static class TextureGeneratorArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(TextureStyle.values()).mapMulti(((generator, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(generator);
                }
            })).map(Arguments::of);
        }
    }

    private static class ResourceGeneratorArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(ResourceStyle.values()).mapMulti(((generator, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(generator);
                }
            })).map(Arguments::of);
        }
    }

    private static class PropGeneratorArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(PropStyle.values()).mapMulti(((generator, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(generator);
                }
            })).map(Arguments::of);
        }
    }

    private static class ValidMapSizeArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return IntStream.iterate(128, size -> size < 512, size -> size + 64).mapToObj(Arguments::of);
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


