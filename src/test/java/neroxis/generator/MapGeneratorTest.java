package neroxis.generator;

import neroxis.map.Army;
import neroxis.map.Group;
import neroxis.map.SCMap;
import neroxis.util.FileUtils;
import neroxis.util.ParseUtils;
import neroxis.util.Pipeline;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static neroxis.util.ImageUtils.compareImages;
import static org.junit.Assert.*;

public class MapGeneratorTest {

    String folderPath = ".";
    String version = MapGenerator.VERSION;
    long seed = 1234;
    byte spawnCount = 2;
    float landDensity = .56746f;
    float plateauDensity = .1324f;
    float mountainDensity = .7956f;
    float rampDensity = .5649f;
    float reclaimDensity = .1354f;
    float mexDensity = .7325f;
    float roundedLandDensity = ParseUtils.discretePercentage(landDensity, 127);
    float roundedPlateauDensity = ParseUtils.discretePercentage(plateauDensity, 127);
    float roundedMountainDensity = ParseUtils.discretePercentage(mountainDensity, 127);
    float roundedRampDensity = ParseUtils.discretePercentage(rampDensity, 127);
    float roundedReclaimDensity = ParseUtils.discretePercentage(reclaimDensity, 127);
    float roundedMexDensity = ParseUtils.discretePercentage(mexDensity, 127);
    int mapSize = 512;
    int numTeams = 2;
    String numericMapName = String.format("neroxis_map_generator_%s_%d", version, seed);
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

    @Before
    public void setup() {
        instance = new MapGenerator();
    }

    @Test
    public void TestParseOrdered3Args() throws Exception {
        String[] args = {folderPath, Long.toString(seed), version};

        instance.interpretArguments(args);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getPathToFolder(), folderPath);
    }

    @Test
    public void TestParseOrdered2Args() throws Exception {
        String[] args = {folderPath, numericMapName};

        instance.interpretArguments(args);

        assertEquals(instance.getMapName(), numericMapName);
        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getPathToFolder(), folderPath);
    }

    @Test
    public void TestParseOrdered4Args() throws Exception {
        String[] args = {folderPath, Long.toString(seed), version, numericMapName};

        instance.interpretArguments(args);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getPathToFolder(), folderPath);
    }

    @Test
    public void TestParseKeywordArgs() throws Exception {
        instance.interpretArguments(keywordArgs);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getPathToFolder(), folderPath);
        assertEquals(instance.getLandDensity(), roundedLandDensity, 0);
        assertEquals(instance.getPlateauDensity(), roundedPlateauDensity, 0);
        assertEquals(instance.getMountainDensity(), roundedMountainDensity, 0);
        assertEquals(instance.getRampDensity(), roundedRampDensity, 0);
        assertEquals(instance.getReclaimDensity(), roundedReclaimDensity, 0);
        assertEquals(instance.getMexDensity(), roundedMexDensity, 0);
        assertEquals(instance.getMapSize(), mapSize);
    }

    @Test
    public void TestDeterminism() throws Exception {
        instance.interpretArguments(keywordArgs);
        SCMap map1 = instance.generate();
        String[] hashArray1 = Pipeline.hashArray.clone();

        for (int i = 0; i < 10; i++) {
            Pipeline.reset();
            instance = new MapGenerator();

            instance.interpretArguments(keywordArgs);
            SCMap map2 = instance.generate();
            String[] hashArray2 = Pipeline.hashArray.clone();

            assertArrayEquals(hashArray1, hashArray2);
            assertEquals(map1.getName(), map2.getName());
            assertEquals(map1.toString(), map2.toString());
            assertEquals(map1.getSpawns(), map2.getSpawns());
            assertEquals(map1.getMexes(), map2.getMexes());
            assertEquals(map1.getHydros(), map2.getHydros());
            assertEquals(map1.getArmies(), map2.getArmies());
            assertEquals(map1.getProps(), map2.getProps());
            assertEquals(map1.getDecals(), map2.getDecals());
            assertEquals(map1.getBiome(), map2.getBiome());
            assertEquals(map1.getSize(), map2.getSize());
            assertTrue(compareImages(map1.getPreview(), map2.getPreview()));
            assertTrue(compareImages(map1.getHeightmap(), map2.getHeightmap()));
            assertTrue(compareImages(map1.getNormalMap(), map2.getNormalMap()));
            assertTrue(compareImages(map1.getTextureMasksHigh(), map2.getTextureMasksHigh()));
            assertTrue(compareImages(map1.getTextureMasksLow(), map2.getTextureMasksLow()));
            assertTrue(compareImages(map1.getWaterMap(), map2.getWaterMap()));
            assertTrue(compareImages(map1.getWaterFoamMask(), map2.getWaterFoamMask()));
            assertTrue(compareImages(map1.getWaterDepthBiasMask(), map2.getWaterDepthBiasMask()));
            assertTrue(compareImages(map1.getWaterFlatnessMask(), map2.getWaterFlatnessMask()));
            assertTrue(compareImages(map1.getTerrainType(), map2.getTerrainType()));
        }
    }

    @Test
    public void TestEqualityMapNameKeyword() throws Exception {
        instance.interpretArguments(keywordArgs);
        SCMap map1 = instance.generate();

        Pipeline.reset();
        instance = new MapGenerator();

        String[] args = {folderPath, map1.getName()};
        instance.interpretArguments(args);
        SCMap map2 = instance.generate();

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
        assertTrue(compareImages(map1.getWaterFoamMask(), map2.getWaterFoamMask()));
        assertTrue(compareImages(map1.getWaterDepthBiasMask(), map2.getWaterDepthBiasMask()));
        assertTrue(compareImages(map1.getWaterFlatnessMask(), map2.getWaterFlatnessMask()));
        assertTrue(compareImages(map1.getTerrainType(), map2.getTerrainType()));
    }

    @Test
    public void TestEqualityMexDensityKeyword() throws Exception {
        instance.interpretArguments(new String[]{"--mex-density", Float.toString(mexDensity)});
        SCMap map1 = instance.generate();

        Pipeline.reset();
        instance = new MapGenerator();

        String[] args = {folderPath, map1.getName()};
        instance.interpretArguments(args);
        SCMap map2 = instance.generate();

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
        assertTrue(compareImages(map1.getWaterFoamMask(), map2.getWaterFoamMask()));
        assertTrue(compareImages(map1.getWaterDepthBiasMask(), map2.getWaterDepthBiasMask()));
        assertTrue(compareImages(map1.getWaterFlatnessMask(), map2.getWaterFlatnessMask()));
        assertTrue(compareImages(map1.getTerrainType(), map2.getTerrainType()));
    }

    @Test
    public void TestEqualityTournamentStyle() throws Exception {
        instance.interpretArguments(new String[]{"--tournament-style"});
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

        assertEquals(map1.getName(), map2.getName());
        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);
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
        assertTrue(compareImages(map1.getWaterFoamMask(), map2.getWaterFoamMask()));
        assertTrue(compareImages(map1.getWaterDepthBiasMask(), map2.getWaterDepthBiasMask()));
        assertTrue(compareImages(map1.getWaterFlatnessMask(), map2.getWaterFlatnessMask()));
        assertTrue(compareImages(map1.getTerrainType(), map2.getTerrainType()));
    }

    @Test
    public void TestInequalityTournamentStyle() throws Exception {
        instance.interpretArguments(new String[]{"--tournament-style"});
        SCMap map1 = instance.generate();
        long generationTime1 = instance.getGenerationTime();
        long seed1 = instance.getSeed();

        Pipeline.reset();
        instance = new MapGenerator();

        instance.interpretArguments(new String[]{"--tournament-style", "--seed", String.valueOf(seed1)});
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
        assertNotEquals(map1.getBiome(), map2.getBiome());
        assertEquals(map1.getSize(), map2.getSize());
        assertFalse(compareImages(map1.getPreview(), map2.getPreview()));
        assertFalse(compareImages(map1.getHeightmap(), map2.getHeightmap()));
        assertFalse(compareImages(map1.getTextureMasksHigh(), map2.getTextureMasksHigh()));
        assertFalse(compareImages(map1.getTextureMasksLow(), map2.getTextureMasksLow()));
    }

    @Test
    public void TestEqualityBlind() throws Exception {
        instance.interpretArguments(new String[]{"--blind"});
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

        assertEquals(map1.getName(), map2.getName());
        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);
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
        assertTrue(compareImages(map1.getWaterFoamMask(), map2.getWaterFoamMask()));
        assertTrue(compareImages(map1.getWaterDepthBiasMask(), map2.getWaterDepthBiasMask()));
        assertTrue(compareImages(map1.getWaterFlatnessMask(), map2.getWaterFlatnessMask()));
        assertTrue(compareImages(map1.getTerrainType(), map2.getTerrainType()));
    }

    @Test
    public void TestEqualityUnexplored() throws Exception {
        instance.interpretArguments(new String[]{"--unexplored"});
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

        assertEquals(map1.getName(), map2.getName());
        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);
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
        assertTrue(compareImages(map1.getWaterFoamMask(), map2.getWaterFoamMask()));
        assertTrue(compareImages(map1.getWaterDepthBiasMask(), map2.getWaterDepthBiasMask()));
        assertTrue(compareImages(map1.getWaterFlatnessMask(), map2.getWaterFlatnessMask()));
        assertTrue(compareImages(map1.getTerrainType(), map2.getTerrainType()));
    }

    @Test
    public void TestEqualityStyleSpecified() throws Exception {
        instance.interpretArguments(new String[]{"--style", "LITTLE_MOUNTAIN"});
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

        assertEquals(map1.getName(), map2.getName());
        assertEquals(generationTime1, generationTime2);
        assertEquals(seed1, seed2);
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
        assertTrue(compareImages(map1.getWaterFoamMask(), map2.getWaterFoamMask()));
        assertTrue(compareImages(map1.getWaterDepthBiasMask(), map2.getWaterDepthBiasMask()));
        assertTrue(compareImages(map1.getWaterFlatnessMask(), map2.getWaterFlatnessMask()));
        assertTrue(compareImages(map1.getTerrainType(), map2.getTerrainType()));
    }

    @Test
    public void TestEqualityMapNameNameKeyword() throws Exception {
        instance.interpretArguments(new String[]{});
        String[] args;
        args = new String[]{"--map-name", instance.getMapName()};
        instance.interpretArguments(args);
        SCMap map1 = instance.generate();

        Pipeline.reset();
        instance = new MapGenerator();

        args = new String[]{folderPath, map1.getName()};
        instance.interpretArguments(args);
        SCMap map2 = instance.generate();

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
        assertTrue(compareImages(map1.getWaterFoamMask(), map2.getWaterFoamMask()));
        assertTrue(compareImages(map1.getWaterDepthBiasMask(), map2.getWaterDepthBiasMask()));
        assertTrue(compareImages(map1.getWaterFlatnessMask(), map2.getWaterFlatnessMask()));
        assertTrue(compareImages(map1.getTerrainType(), map2.getTerrainType()));
    }

    @Test
    public void TestUnexploredNoUnits() throws Exception {
        for (int i = 0; i < 5; ++i) {
            Pipeline.reset();
            instance = new MapGenerator();
            instance.interpretArguments(new String[]{"--unexplored"});
            SCMap map = instance.generate();

            for (Army army : map.getArmies()) {
                for (Group group : army.getGroups()) {
                    assertEquals(0, group.getUnits().size());
                }
            }
        }
    }

    @After
    public void cleanup() {
        Pipeline.reset();
        FileUtils.deleteRecursiveIfExists(Paths.get(instance.getMapName()));
    }

}


