package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.exporter.MapExporter;
import com.faforever.neroxis.exporter.PreviewGenerator;
import com.faforever.neroxis.generator.cli.ParameterOptions;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import picocli.CommandLine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.faforever.neroxis.util.ImageUtil.compareImages;
import static org.junit.jupiter.api.Assertions.*;

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
    float roundedLandDensity = MathUtil.discretePercentage(landDensity, 127);
    float roundedPlateauDensity = MathUtil.discretePercentage(plateauDensity, 127);
    float roundedMountainDensity = MathUtil.discretePercentage(mountainDensity, 127);
    float roundedRampDensity = MathUtil.discretePercentage(rampDensity, 127);
    float roundedReclaimDensity = MathUtil.discretePercentage(reclaimDensity, 127);
    float roundedMexDensity = MathUtil.discretePercentage(mexDensity, 127);
    int mapSize = 256;
    int numTeams = 2;
    String[] keywordArgs;
    private MapGenerator instance;

    @BeforeEach
    public void setup() {
        keywordArgs = new String[]{"--seed", Long.toString(seed), "--spawn-count", Byte.toString(
                spawnCount), "--land-density", Float.toString(landDensity), "--plateau-density", Float.toString(
                plateauDensity), "--mountain-density", Float.toString(
                mountainDensity), "--ramp-density", Float.toString(rampDensity), "--reclaim-density", Float.toString(
                reclaimDensity), "--mex-density", Float.toString(mexDensity), "--map-size", Integer.toString(
                mapSize), "--num-teams", Integer.toString(numTeams)};

        instance = new MapGenerator();
    }

    @Test
    public void TestParseMapName() {
        new CommandLine(instance).execute("--map-name", mapName);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getOutputFolderMixin().getOutputPath(), Path.of("."));
        assertEquals(instance.getGeneratorParameters().getLandDensity(), roundedLandDensity);
        assertEquals(instance.getGeneratorParameters().getPlateauDensity(), roundedPlateauDensity);
        assertEquals(instance.getGeneratorParameters().getMountainDensity(), roundedMountainDensity);
        assertEquals(instance.getGeneratorParameters().getRampDensity(), roundedRampDensity);
        assertEquals(instance.getGeneratorParameters().getReclaimDensity(), roundedReclaimDensity);
        assertEquals(instance.getGeneratorParameters().getMexDensity(), roundedMexDensity);
        assertEquals(instance.getGeneratorParameters().getMapSize(), mapSize);
    }

    @Test
    public void TestParseKeywordArgs() {
        new CommandLine(instance).execute(keywordArgs);

        ParameterOptions parameterOptions = instance.getTuningOptions().getParameterOptions();
        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getOutputFolderMixin().getOutputPath(), Path.of("."));
        assertEquals(parameterOptions.getLandDensity(), roundedLandDensity);
        assertEquals(parameterOptions.getPlateauDensity(), roundedPlateauDensity);
        assertEquals(parameterOptions.getMountainDensity(), roundedMountainDensity);
        assertEquals(parameterOptions.getRampDensity(), roundedRampDensity);
        assertEquals(parameterOptions.getReclaimDensity(), roundedReclaimDensity);
        assertEquals(parameterOptions.getMexDensity(), roundedMexDensity);
        assertEquals(instance.getNumTeams(), numTeams);
        assertEquals(instance.getMapSize(), mapSize);
        assertEquals(instance.getMapName(), mapName);
    }

    @Test
    public void TestParseMapSizes() {
        for (int i = 0; i < 2048; i += 64) {
            new CommandLine(instance).parseArgs("--map-size", String.valueOf(i));

            assertEquals(StrictMath.round(i / 64f) * 64, instance.getMapSize());
        }
    }

    @Test
    public void TestMapExportedToProperSize() {
        new CommandLine(instance).execute("--map-size", "384");

        SCMap map = instance.getMap();

        MapExporter.exportMap(Paths.get("."), map, false, false);

        assertEquals(512, map.getSize());
    }

    @Test
    public void TestDeterminism() {
        new CommandLine(instance).execute(keywordArgs);
        SCMap map1 = instance.getMap();
        String[] hashArray1 = Pipeline.getHashArray().clone();

        for (int i = 0; i < 5; i++) {
            instance = new MapGenerator();

            new CommandLine(instance).execute(keywordArgs);
            SCMap map2 = instance.getMap();
            String[] hashArray2 = Pipeline.getHashArray().clone();

            assertSCMapEquality(map1, map2);
            assertArrayEquals(hashArray1, hashArray2);
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
        assertTrue(compareImages(map1.getWaterFlatnessMap(), map2.getWaterFlatnessMap()));
        assertTrue(compareImages(map1.getTerrainType(), map2.getTerrainType()));
    }

    @Test
    public void TestMultipleGenerationDeterminism() {
        for (int i = 0; i < 3; i++) {
            instance = new MapGenerator();
            new CommandLine(instance).execute("--num-to-generate", "2", "--map-size", "256");
            SCMap map1 = instance.getMap();
            String[] hashArray1 = Pipeline.getHashArray().clone();

            instance = new MapGenerator();
            new CommandLine(instance).execute("--map-name", map1.getName());
            SCMap map2 = instance.getMap();
            String[] hashArray2 = Pipeline.getHashArray().clone();

            assertArrayEquals(hashArray1, hashArray2);
            assertSCMapEquality(map1, map2);
        }
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

    @Test
    public void TestEqualityStyleSpecified() {
        MapStyle[] styles = MapStyle.values();
        for (MapStyle style : styles) {
            for (int i = 0; i < 3; i++) {
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
        }
    }

    @Test
    public void TestEqualityBiomeSpecified() {
        for (String name : Biomes.BIOMES_LIST) {
            for (int i = 0; i < 3; i++) {
                instance = new MapGenerator();

                new CommandLine(instance).execute("--biome", name, "--map-size", "256");
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
        }
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

    @Test
    public void TestMultiVisibilityOptionsFail() {
        instance = new MapGenerator();
        assertThrows(CommandLine.ParameterException.class,
                () -> new CommandLine(instance).parseArgs("--unexplored", "--blind"));
        assertThrows(CommandLine.ParameterException.class,
                () -> new CommandLine(instance).parseArgs("--tournament-style", "--blind"));
        assertThrows(CommandLine.ParameterException.class,
                () -> new CommandLine(instance).parseArgs("--unexplored", "--tournament-style"));
        assertThrows(CommandLine.ParameterException.class,
                () -> new CommandLine(instance).parseArgs("--visibility", "BLIND", "--blind"));
        assertThrows(CommandLine.ParameterException.class,
                () -> new CommandLine(instance).parseArgs("--visibility", "TOURNAMENT_STYLE",
                        "--tournament-style"));
        assertThrows(CommandLine.ParameterException.class,
                () -> new CommandLine(instance).parseArgs("--visibility", "UNEXPLORED", "--unexplored"));
    }

    @Test
    public void TestMultiTuningOptionsFail() {
        instance = new MapGenerator();
        assertThrows(CommandLine.ParameterException.class,
                () -> new CommandLine(instance).parseArgs("--unexplored", "--style", "TEST"));
        assertThrows(CommandLine.ParameterException.class,
                () -> new CommandLine(instance).parseArgs("--unexplored", "--land-density", "1"));
        assertThrows(CommandLine.ParameterException.class,
                () -> new CommandLine(instance).parseArgs("--land-density", "1", "--style", "TEST"));
    }

    @AfterEach
    public void cleanup() throws IOException {
        DebugUtil.DEBUG = false;
        Files.list(Path.of("."))
             .filter(path -> path.getFileName().toString().startsWith("neroxis_map_generator_snapshot"))
             .forEach(FileUtil::deleteRecursiveIfExists);
    }
}


