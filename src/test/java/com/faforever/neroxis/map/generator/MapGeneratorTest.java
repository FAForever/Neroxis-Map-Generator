package com.faforever.neroxis.map.generator;

import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.evaluator.MapEvaluator;
import com.faforever.neroxis.map.exporter.MapExporter;
import com.faforever.neroxis.map.generator.style.StyleGenerator;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.FileUtils;
import com.faforever.neroxis.util.ImageUtils;
import com.faforever.neroxis.util.MathUtils;
import com.faforever.neroxis.util.Pipeline;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static com.faforever.neroxis.util.ImageUtils.compareImages;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
public class MapGeneratorTest {

    String folderPath = ".";
    long seed = 1234;
    byte spawnCount = 2;
    float landDensity = .56746f;
    float plateauDensity = .1324f;
    float mountainDensity = .7956f;
    float rampDensity = .5649f;
    float reclaimDensity = .1354f;
    float mexDensity = .7325f;
    float roundedLandDensity = MathUtils.discretePercentage(landDensity, 127);
    float roundedPlateauDensity = MathUtils.discretePercentage(plateauDensity, 127);
    float roundedMountainDensity = MathUtils.discretePercentage(mountainDensity, 127);
    float roundedRampDensity = MathUtils.discretePercentage(rampDensity, 127);
    float roundedReclaimDensity = MathUtils.discretePercentage(reclaimDensity, 127);
    float roundedMexDensity = MathUtils.discretePercentage(mexDensity, 127);
    int mapSize = 256;
    int numTeams = 2;
    String[] keywordArgs = {"--folder-path", ".",
            "--seed", Long.toString(seed),
            "--spawn-count", Byte.toString(spawnCount),
            "--land-density", Float.toString(landDensity),
            "--plateau-density", Float.toString(plateauDensity),
            "--mountain-density", Float.toString(mountainDensity),
            "--ramp-density", Float.toString(rampDensity),
            "--reclaim-density", Float.toString(reclaimDensity),
            "--mex-density", Float.toString(mexDensity),
            "--map-size", Integer.toString(mapSize),
            "--num-teams", Integer.toString(numTeams)};
    private MapGenerator instance;

    @BeforeEach
    public void setup() {
        instance = new MapGenerator();
    }

    @Test
    public void TestParseKeywordArgs() throws Exception {
        instance.interpretArguments(keywordArgs);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getPathToFolder(), folderPath);
        assertEquals(instance.getLandDensity(), roundedLandDensity);
        assertEquals(instance.getPlateauDensity(), roundedPlateauDensity);
        assertEquals(instance.getMountainDensity(), roundedMountainDensity);
        assertEquals(instance.getRampDensity(), roundedRampDensity);
        assertEquals(instance.getReclaimDensity(), roundedReclaimDensity);
        assertEquals(instance.getMexDensity(), roundedMexDensity);
        assertEquals(instance.getMapSize(), mapSize);
    }

    @Test
    public void TestParseMapSizes() throws Exception {
        for (int i = 0; i < 2048; ++i) {
            instance.interpretArguments(new String[]{"--map-size", String.valueOf(i)});

            assertEquals(StrictMath.round(i / 64f) * 64, instance.getMapSize());
        }
    }

    @Test
    public void TestMapExportedToProperSize() throws Exception {
        instance.interpretArguments(new String[]{"--map-size", "384"});

        SCMap map = instance.generate();

        MapExporter.exportMap(Paths.get("."), map, false, false, false);

        assertEquals(512, map.getSize());
    }


    @Test
    public void TestDeterminism() throws Exception {
        instance.interpretArguments(keywordArgs);
        SCMap map1 = instance.generate();
        String[] hashArray1 = Pipeline.getHashArray().clone();

        for (int i = 0; i < 5; i++) {
            Pipeline.reset();
            instance = new MapGenerator();

            instance.interpretArguments(keywordArgs);
            SCMap map2 = instance.generate();
            String[] hashArray2 = Pipeline.getHashArray().clone();

            assertArrayEquals(hashArray1, hashArray2);
            assertSCMapEquality(map1, map2);
        }
    }

    @Test
    public void TestEqualityMapNameKeyword() throws Exception {
        instance.interpretArguments(keywordArgs);
        SCMap map1 = instance.generate();

        Pipeline.reset();
        instance = new MapGenerator();

        String[] args = {"--map-name", map1.getName()};
        instance.interpretArguments(args);
        SCMap map2 = instance.generate();

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestEqualityWithVisualizationMapNameKeyword() throws Exception {
        instance.interpretArguments(keywordArgs);
        SCMap map1 = instance.generate();

        Pipeline.reset();
        instance = new MapGenerator();

        String[] args = {"--map-name", map1.getName(), "--visualize"};
        instance.interpretArguments(args);
        SCMap map2 = instance.generate();

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestEqualityTournamentStyle() throws Exception {
        instance.interpretArguments(new String[]{"--tournament-style", "--map-size", "256"});
        SCMap map1 = instance.generate();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getSeed();

        Pipeline.reset();
        instance = new MapGenerator();

        instance.interpretArguments(new String[]{"--map-name", mapName});
        SCMap map2 = instance.generate();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestInequalityTournamentStyle() throws Exception {
        instance.interpretArguments(new String[]{"--tournament-style", "--map-size", "256"});
        SCMap map1 = instance.generate();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getSeed();

        Pipeline.reset();
        Thread.sleep(1000);
        instance = new MapGenerator();

        instance.interpretArguments(new String[]{"--tournament-style", "--seed", String.valueOf(seed1), "--map-size", "256"});
        SCMap map2 = instance.generate();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getSeed();

        assertNotEquals(map1.getName(), map2.getName());
        assertNotEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);
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
    public void TestEqualityBlind() throws Exception {
        instance.interpretArguments(new String[]{"--blind", "--map-size", "256"});
        SCMap map1 = instance.generate();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getSeed();

        Pipeline.reset();
        instance = new MapGenerator();

        instance.interpretArguments(new String[]{"--map-name", mapName});
        SCMap map2 = instance.generate();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestEqualityUnexplored() throws Exception {
        instance.interpretArguments(new String[]{"--unexplored", "--map-size", "256"});
        SCMap map1 = instance.generate();
        String mapName = instance.getMapName();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getSeed();

        Pipeline.reset();
        instance = new MapGenerator();

        instance.interpretArguments(new String[]{"--map-name", mapName});
        SCMap map2 = instance.generate();
        long generationTime2 = instance.getGenerationTime();
        long seed2 = instance.getSeed();

        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);

        assertSCMapEquality(map1, map2);
    }

    @Test
    public void TestEqualityStyleSpecified() throws Exception {
        List<StyleGenerator> styles = instance.getMapStyles();
        for (StyleGenerator styleGenerator : styles) {
            Pipeline.reset();
            instance = new MapGenerator();

            instance.interpretArguments(new String[]{"--style", styleGenerator.getName(), "--map-size", "256"});
            SCMap map1 = instance.generate();
            String mapName = instance.getMapName();
            long generationTime1 = instance.getGenerationTime();
            long seed1 = instance.getSeed();

            Pipeline.reset();
            instance = new MapGenerator();

            instance.interpretArguments(new String[]{"--map-name", mapName});
            SCMap map2 = instance.generate();
            long generationTime2 = instance.getGenerationTime();
            long seed2 = instance.getSeed();

            assertEquals(generationTime1, generationTime2);
            assertEquals(seed1, seed2);

            assertSCMapEquality(map1, map2);
        }
    }

    @Test
    public void TestEqualityBiomeSpecified() throws Exception {
        for (String name : Biomes.BIOMES_LIST) {
            Pipeline.reset();
            instance = new MapGenerator();

            instance.interpretArguments(new String[]{"--biome", name, "--map-size", "256"});
            SCMap map1 = instance.generate();
            String mapName = instance.getMapName();
            long generationTime1 = instance.getGenerationTime();
            long seed1 = instance.getSeed();

            Pipeline.reset();
            instance = new MapGenerator();

            instance.interpretArguments(new String[]{"--map-name", mapName});
            SCMap map2 = instance.generate();
            long generationTime2 = instance.getGenerationTime();
            long seed2 = instance.getSeed();

            assertEquals(generationTime1, generationTime2);
            assertEquals(seed1, seed2);

            assertSCMapEquality(map1, map2);
        }
    }

//    @Test
//    public void TestStyleSymmetry() throws Exception {
//        List<StyleGenerator> styles = instance.getMapStyles();
//        for (StyleGenerator styleGenerator : styles) {
//            for (int i = 0; i < 5; ++i) {
//                Pipeline.reset();
//                instance = new MapGenerator();
//
//                instance.interpretArguments(new String[]{"--style", styleGenerator.getName(), "--map-size", "256"});
//                SCMap map1 = instance.generate();
//                assertSCMapSymmetry(map1, instance.getSymmetrySettings(), styleGenerator.getName());
//            }
//        }
//    }

    @Test
    public void TestUnexploredNoUnits() throws Exception {
        for (int i = 0; i < 10; ++i) {
            Pipeline.reset();
            instance = new MapGenerator();
            instance.interpretArguments(new String[]{"--unexplored", "--map-size", "256"});
            SCMap map = instance.generate();

            for (Army army : map.getArmies()) {
                for (Group group : army.getGroups()) {
                    assertEquals(0, group.getUnits().size());
                }
            }
        }
    }

    @Test
    public void TestUnexploredPreview() throws Exception {
        Pipeline.reset();
        instance = new MapGenerator();
        instance.interpretArguments(new String[]{"--unexplored", "--map-size", "256"});
        SCMap map = instance.generate();

        BufferedImage blankPreview = ImageUtils.readImage(PreviewGenerator.BLANK_PREVIEW);
        BufferedImage mapPreview = map.getPreview();

        assertArrayEquals(blankPreview.getRGB(0, 0, 256, 256, null, 0, 256),
                mapPreview.getRGB(0, 0, 256, 256, null, 0, 256));
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

    private void assertSCMapSymmetry(SCMap map, SymmetrySettings symmetrySettings, String name) {
        FloatMask heightMask = new FloatMask(map.getHeightmap(), null, symmetrySettings, map.getHeightMapScale());
        assertEquals(0, MapEvaluator.getMaskScore(heightMask));
        assertEquals(0, MapEvaluator.getPositionedObjectScore(map.getSpawns(), heightMask));
        assertEquals(0, MapEvaluator.getPositionedObjectScore(map.getMexes(), heightMask));
        assertEquals(0, MapEvaluator.getPositionedObjectScore(map.getHydros(), heightMask));
        assertEquals(0, MapEvaluator.getPositionedObjectScore(map.getProps(), heightMask));
        assertEquals(0, MapEvaluator.getPositionedObjectScore(map.getArmies().stream().flatMap(army -> army.getGroups().stream()
                .flatMap(group -> group.getUnits().stream())).collect(Collectors.toList()), heightMask));
    }

    @AfterEach
    public void cleanup() {
        Pipeline.reset();
        FileUtils.deleteRecursiveIfExists(Paths.get(instance.getMapName()));
    }

}


