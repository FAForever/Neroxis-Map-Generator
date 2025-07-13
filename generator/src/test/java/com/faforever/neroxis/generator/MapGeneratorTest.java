package com.faforever.neroxis.generator;

import com.faforever.neroxis.exporter.PreviewGenerator;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.util.ImageUtil;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import picocli.CommandLine;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
public class MapGeneratorTest {
    public static final int NUM_DETERMINISM_REPEATS = 3;

    private final String[] keywordArgs = new String[]{"--seed", Long.toString(1234), "--spawn-count", Byte.toString(
            (byte) 2), "--terrain-style", TerrainStyle.BIG_ISLANDS.name(), "--texture-style", TextureStyle.BRIMSTONE.name(), "--resource-style", ResourceStyle.LOW_MEX.name(), "--prop-style", PropStyle.ENEMY_CIV.name(), "--terrain-symmetry", Symmetry.XZ.name(), "--map-size", Integer.toString(
            256), "--resource-density", Float.toString(.7325f), "--reclaim-density", Float.toString(
            .1354f), "--num-teams", Integer.toString(2)};

    @ParameterizedTest
    @ArgumentsSource(ValidTerrainAndMapSizeArgumentProvider.class)
    public void TestAllTerrainsGenerateAllSizes(TerrainStyle terrainStyle, int mapSize) {
        MapGenerator instance = new MapGenerator(true);
        new CommandLine(instance).execute("--terrain-style", terrainStyle.toString(), "--map-size",
                                          String.valueOf(mapSize), "--spawn-count", "2");

        SCMap map = instance.getMap();

        assertTrue(map.getDescription().contains(terrainStyle.getGeneratorSupplier().get().getClass().getSimpleName()),
                   map.getDescription() + " doesn't contain " + terrainStyle.getGeneratorSupplier()
                                                                            .get()
                                                                            .getClass()
                                                                            .getSimpleName());
        assertEquals(mapSize, instance.getGeneratorParameters().mapSize());
        assertEquals(mapSize, map.getPlayableArea().z() - map.getPlayableArea().x());
    }

    @ParameterizedTest
    @ArgumentsSource(ValidMapSizeArgumentProvider.class)
    public void TestMapExportedToProperSize(int mapSize) {
        MapGenerator instance = new MapGenerator(true);
        new CommandLine(instance).execute("--map-size", String.valueOf(mapSize), "--spawn-count", "2");

        SCMap map = instance.getMap();

        assertEquals(0, (StrictMath.log(map.getSize()) / StrictMath.log(2)) % 1);
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestDeterminism() throws IOException {
        MapGenerator instance1 = new MapGenerator(true);
        new CommandLine(instance1).execute(keywordArgs);
        assertEquals(instance1.getGeneratorParameters(), instance1.getStyleGenerator().getGeneratorParameters());

        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute(keywordArgs);
        assertEquals(instance2.getGeneratorParameters(), instance2.getStyleGenerator().getGeneratorParameters());

        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestMultipleGenerationDeterminism() throws IOException {
        MapGenerator instance1 = new MapGenerator(true);
        new CommandLine(instance1).execute("--num-to-generate", "2", "--map-size", "256", "--spawn-count", "2");
        assertEquals(instance1.getGeneratorParameters(), instance1.getStyleGenerator().getGeneratorParameters());
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();

        MapGenerator instance2 = new MapGenerator(true);
        new CommandLine(instance2).execute("--map-name", map1.getName());
        assertEquals(instance2.getGeneratorParameters(), instance2.getStyleGenerator().getGeneratorParameters());
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestEqualityMapNameKeyword() throws IOException {
        MapGenerator instance1 = new MapGenerator(true);
        new CommandLine(instance1).execute(keywordArgs);
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();

        MapGenerator instance2 = new MapGenerator(true);

        String[] args = {"--map-name", map1.getName()};
        new CommandLine(instance2).execute(args);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityWithDebugMapNameKeyword() throws IOException {
        MapGenerator instance1 = new MapGenerator(true);
        new CommandLine(instance1).execute(keywordArgs);
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString()
                                             .lines()
                                             .map(line -> line.split(","))
                                             .map(line -> line[0] + ", " + line[2])
                                             .collect(Collectors.joining("\n"));

        MapGenerator instance2 = new MapGenerator(true);

        String[] args = {"--map-name", map1.getName(), "--debug"};
        new CommandLine(instance2).execute(args);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString()
                                             .lines()
                                             .map(line -> line.split(","))
                                             .map(line -> line[0] + ", " + line[2])
                                             .collect(Collectors.joining("\n"));

        assertEquals(hashArray1, hashArray2);

        assertSCMapEquality(map1, map2);
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityTournamentStyle() throws IOException {
        MapGenerator instance1 = new MapGenerator(true);
        new CommandLine(instance1).execute("--tournament-style", "--map-size", "256", "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = instance1.getMapName();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--map-name", mapName);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestInequalityTournamentStyle() throws Exception {
        MapGenerator instance1 = new MapGenerator(true);
        new CommandLine(instance1).execute("--tournament-style", "--map-size", "256", "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        Thread.sleep(1000);
        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--tournament-style", "--seed", String.valueOf(seed1), "--map-size", "256",
                                           "--spawn-count", "2");
        SCMap map2 = instance2.getMap();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertNotEquals(map1.getName(), map2.getName());
        assertNotEquals(generationTime1, generationTime2);
        assertNotEquals(seed1, seed2);
        assertNotEquals(map1.getSpawns(), map2.getSpawns());
        assertNotEquals(map1.getMexes(), map2.getMexes());
        assertNotEquals(map1.getHydros(), map2.getHydros());
        assertNotEquals(map1.getProps(), map2.getProps());
        assertEquals(map1.getSize(), map2.getSize());
        assertFalse(Arrays.equals(ImageUtil.getImagePixels(map1.getPreview()),
                                  ImageUtil.getImagePixels(map2.getPreview())));
        assertFalse(Arrays.equals(ImageUtil.getImagePixels(map1.getHeightmap()),
                                  ImageUtil.getImagePixels(map2.getHeightmap())));
        assertFalse(Arrays.equals(ImageUtil.getImagePixels(map1.getTextureMasksHigh()),
                                  ImageUtil.getImagePixels(map2.getTextureMasksHigh())));
        assertFalse(Arrays.equals(ImageUtil.getImagePixels(map1.getTextureMasksLow()),
                                  ImageUtil.getImagePixels(map2.getTextureMasksLow())));
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityBlind() throws IOException {
        MapGenerator instance1 = new MapGenerator(true);
        new CommandLine(instance1).execute("--blind", "--map-size", "256", "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = instance1.getMapName();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--map-name", mapName);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestInequalityBlind() throws Exception {
        MapGenerator instance1 = new MapGenerator(true);
        new CommandLine(instance1).execute("--blind", "--map-size", "256", "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        Thread.sleep(1000);
        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--blind", "--seed", String.valueOf(seed1), "--map-size", "256",
                                           "--spawn-count", "2");
        SCMap map2 = instance2.getMap();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertNotEquals(map1.getName(), map2.getName());
        assertNotEquals(generationTime1, generationTime2);
        assertNotEquals(seed1, seed2);
        assertNotEquals(map1.getSpawns(), map2.getSpawns());
        assertNotEquals(map1.getMexes(), map2.getMexes());
        assertNotEquals(map1.getHydros(), map2.getHydros());
        assertNotEquals(map1.getProps(), map2.getProps());
        assertEquals(map1.getSize(), map2.getSize());
        assertArrayEquals(ImageUtil.getImagePixels(map1.getPreview()), ImageUtil.getImagePixels(map2.getPreview()));
        assertFalse(Arrays.equals(ImageUtil.getImagePixels(map1.getHeightmap()),
                                  ImageUtil.getImagePixels(map2.getHeightmap())));
        assertFalse(Arrays.equals(ImageUtil.getImagePixels(map1.getTextureMasksHigh()),
                                  ImageUtil.getImagePixels(map2.getTextureMasksHigh())));
        assertFalse(Arrays.equals(ImageUtil.getImagePixels(map1.getTextureMasksLow()),
                                  ImageUtil.getImagePixels(map2.getTextureMasksLow())));
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityUnexplored() throws IOException {
        MapGenerator instance1 = new MapGenerator(true);
        new CommandLine(instance1).execute("--unexplored", "--map-size", "256", "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = instance1.getMapName();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--map-name", mapName);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestInequalityUnexplored() throws Exception {
        MapGenerator instance1 = new MapGenerator(true);
        new CommandLine(instance1).execute("--unexplored", "--map-size", "256", "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        Thread.sleep(1000);
        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--unexplored", "--seed", String.valueOf(seed1), "--map-size", "256",
                                           "--spawn-count", "2");
        SCMap map2 = instance2.getMap();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertNotEquals(map1.getName(), map2.getName());
        assertNotEquals(generationTime1, generationTime2);
        assertNotEquals(seed1, seed2);
        assertNotEquals(map1.getSpawns(), map2.getSpawns());
        assertNotEquals(map1.getMexes(), map2.getMexes());
        assertNotEquals(map1.getHydros(), map2.getHydros());
        assertNotEquals(map1.getProps(), map2.getProps());
        assertEquals(map1.getSize(), map2.getSize());
        assertArrayEquals(ImageUtil.getImagePixels(map1.getPreview()), ImageUtil.getImagePixels(map2.getPreview()));
        assertFalse(Arrays.equals(ImageUtil.getImagePixels(map1.getHeightmap()),
                                  ImageUtil.getImagePixels(map2.getHeightmap())));
        assertFalse(Arrays.equals(ImageUtil.getImagePixels(map1.getTextureMasksHigh()),
                                  ImageUtil.getImagePixels(map2.getTextureMasksHigh())));
        assertFalse(Arrays.equals(ImageUtil.getImagePixels(map1.getTextureMasksLow()),
                                  ImageUtil.getImagePixels(map2.getTextureMasksLow())));
    }

    @ParameterizedTest
    @ArgumentsSource(MapStyleArgumentProvider.class)
    public void TestEqualityStyleSpecified(MapStyle style) throws IOException {
        MapGenerator instance1 = new MapGenerator(true);

        new CommandLine(instance1).execute("--style", style.toString(), "--map-size", "256", "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = instance1.getMapName();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--map-name", mapName);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(SymmetryArgumentProvider.class)
    public void TestEqualitySymmetrySpecified(Symmetry symmetry) throws IOException {
        MapGenerator instance1 = new MapGenerator(true);

        int numTeams = switch (symmetry) {
            case Symmetry s when s.getNumSymPoints() == 1 -> 0;
            case Symmetry s when s.getNumSymPoints() % 2 == 0 -> 2;
            case Symmetry s when s.getNumSymPoints() % 3 == 0 -> 3;
            case Symmetry s -> s.getNumSymPoints();
        };

        int spawnCount = numTeams == 0 ? 4 : numTeams;

        new CommandLine(instance1).execute("--terrain-symmetry", symmetry.toString(), "--map-size", "512",
                                           "--num-teams", String.valueOf(numTeams), "--spawn-count",
                                           String.valueOf(spawnCount));
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = instance1.getMapName();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--map-name", mapName);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(TerrainGeneratorArgumentProvider.class)
    public void TestEqualityTerrainGeneratorSpecified(TerrainStyle terrainStyle) throws IOException {
        MapGenerator instance1 = new MapGenerator(true);

        new CommandLine(instance1).execute("--terrain-style", terrainStyle.toString(), "--map-size", "256",
                                           "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = instance1.getMapName();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--map-name", mapName);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(TextureGeneratorArgumentProvider.class)
    public void TestEqualityTextureGeneratorSpecified(TextureStyle textureStyle) throws IOException {
        MapGenerator instance1 = new MapGenerator(true);

        new CommandLine(instance1).execute("--texture-style", textureStyle.toString(), "--map-size", "256",
                                           "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = instance1.getMapName();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--map-name", mapName);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(ResourceGeneratorArgumentProvider.class)
    public void TestEqualityResourceGeneratorSpecified(ResourceStyle resourceStyle) throws IOException {
        MapGenerator instance1 = new MapGenerator(true);

        new CommandLine(instance1).execute("--resource-style", resourceStyle.toString(), "--map-size", "256",
                                           "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = instance1.getMapName();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--map-name", mapName);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @ParameterizedTest
    @ArgumentsSource(PropGeneratorArgumentProvider.class)
    public void TestEqualityPropGeneratorSpecified(PropStyle propStyle) throws IOException {
        MapGenerator instance1 = new MapGenerator(true);

        new CommandLine(instance1).execute("--prop-style", propStyle.toString(), "--map-size", "256", "--spawn-count",
                                           "2");
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = instance1.getMapName();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--map-name", mapName);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityReclaimDensitySpecified() throws IOException {
        MapGenerator instance1 = new MapGenerator(true);

        new CommandLine(instance1).execute("--reclaim-density", String.valueOf(new Random().nextFloat()), "--map-size",
                                           "256", "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = instance1.getMapName();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--map-name", mapName);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @RepeatedTest(NUM_DETERMINISM_REPEATS)
    public void TestEqualityResourceDensitySpecified() throws IOException {
        MapGenerator instance1 = new MapGenerator(true);

        new CommandLine(instance1).execute("--resource-density", String.valueOf(new Random().nextFloat()), "--map-size",
                                           "256", "--spawn-count", "2");
        SCMap map1 = instance1.getMap();
        ByteArrayOutputStream hash1OutputStream = new ByteArrayOutputStream();
        instance1.getStyleGenerator().writePipelines(hash1OutputStream);
        String hashArray1 = hash1OutputStream.toString();
        String mapName = instance1.getMapName();
        long generationTime1 = instance1.getGenerationTime();
        long seed1 = instance1.getBasicOptions().getSeed();

        MapGenerator instance2 = new MapGenerator(true);

        new CommandLine(instance2).execute("--map-name", mapName);
        SCMap map2 = instance2.getMap();
        ByteArrayOutputStream hash2OutputStream = new ByteArrayOutputStream();
        instance2.getStyleGenerator().writePipelines(hash2OutputStream);
        String hashArray2 = hash2OutputStream.toString();
        long generationTime2 = instance2.getGenerationTime();
        long seed2 = instance2.getBasicOptions().getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertEquals(hashArray1, hashArray2);
        assertSCMapEquality(map1, map2);
    }

    @RepeatedTest(10)
    public void TestUnexploredNoUnits() {
        MapGenerator instance = new MapGenerator(true);
        new CommandLine(instance).execute("--unexplored", "--map-size", "256", "--spawn-count", "2");
        SCMap map = instance.getMap();

        for (Army army : map.getArmies()) {
            for (Group group : army.getGroups()) {
                assertEquals(0, group.getUnits().size());
            }
        }
    }

    @Test
    public void TestUnexploredPreview() throws Exception {
        MapGenerator instance = new MapGenerator(true);
        new CommandLine(instance).execute("--unexplored", "--map-size", "256", "--spawn-count", "2");
        SCMap map = instance.getMap();

        BufferedImage blankPreview = ImageUtil.readImage(PreviewGenerator.BLANK_PREVIEW);
        BufferedImage mapPreview = map.getPreview();

        assertArrayEquals(blankPreview.getRGB(0, 0, 256, 256, null, 0, 256),
                          mapPreview.getRGB(0, 0, 256, 256, null, 0, 256));
    }

    private static class SymmetryArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations,
                                                            ExtensionContext context) {
            return Arrays.stream(Symmetry.values()).mapMulti(((symmetry, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(symmetry);
                }
            })).map(Arguments::of);
        }
    }

    private static class MapStyleArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations,
                                                            ExtensionContext context) {
            return Arrays.stream(MapStyle.values()).mapMulti(((mapStyle, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(mapStyle);
                }
            })).map(Arguments::of);
        }
    }

    private static class TerrainGeneratorArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations,
                                                            ExtensionContext context) {
            return Arrays.stream(TerrainStyle.values()).mapMulti(((generator, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(generator);
                }
            })).map(Arguments::of);
        }
    }

    private static class TextureGeneratorArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations,
                                                            ExtensionContext context) {
            return Arrays.stream(TextureStyle.values()).mapMulti(((generator, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(generator);
                }
            })).map(Arguments::of);
        }
    }

    private static class ResourceGeneratorArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations,
                                                            ExtensionContext context) {
            return Arrays.stream(ResourceStyle.values()).mapMulti(((generator, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(generator);
                }
            })).map(Arguments::of);
        }
    }

    private static class PropGeneratorArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations,
                                                            ExtensionContext context) {
            return Arrays.stream(PropStyle.values()).mapMulti(((generator, consumer) -> {
                for (int i = 0; i < NUM_DETERMINISM_REPEATS; i++) {
                    consumer.accept(generator);
                }
            })).map(Arguments::of);
        }
    }

    private static class ValidMapSizeArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations,
                                                            ExtensionContext context) {
            return IntStream.iterate(256, size -> size < 512, size -> size + 64).mapToObj(Arguments::of);
        }
    }

    private static class ValidTerrainAndMapSizeArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameterDeclarations,
                                                            ExtensionContext context) {
            ArrayList<Arguments> arguments = new ArrayList<>();
            for (TerrainStyle terrainStyle : TerrainStyle.values()) {
                for (int size = 256; size <= 512; size += 64) {
                    arguments.add(Arguments.of(terrainStyle, size));
                }
            }
            return arguments.stream();
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
        assertArrayEquals(ImageUtil.getImagePixels(map1.getPreview()), ImageUtil.getImagePixels(map2.getPreview()));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getHeightmap()), ImageUtil.getImagePixels(map2.getHeightmap()));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getNormalMap()), ImageUtil.getImagePixels(map2.getNormalMap()));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getTextureMasksHigh()),
                          ImageUtil.getImagePixels(map2.getTextureMasksHigh()));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getTextureMasksLow()),
                          ImageUtil.getImagePixels(map2.getTextureMasksLow()));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getWaterMap()), ImageUtil.getImagePixels(map2.getWaterMap()));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getWaterFoamMap()),
                          ImageUtil.getImagePixels(map2.getWaterFoamMap()));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getWaterDepthBiasMap()),
                          ImageUtil.getImagePixels(map2.getWaterDepthBiasMap()));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getWaterShadowMap()),
                          ImageUtil.getImagePixels(map2.getWaterShadowMap()));
        assertArrayEquals(ImageUtil.getImagePixels(map1.getTerrainType()),
                          ImageUtil.getImagePixels(map2.getTerrainType()));
    }
}


