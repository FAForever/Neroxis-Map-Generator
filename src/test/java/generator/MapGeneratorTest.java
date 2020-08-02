package generator;

import com.google.common.io.BaseEncoding;
import map.SCMap;
import map.Symmetry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.FileUtils;
import util.Pipeline;

import java.nio.ByteBuffer;
import java.nio.file.Paths;

import static generator.MapGenerator.*;
import static org.junit.Assert.*;
import static util.ImageUtils.compareImages;

public class MapGeneratorTest {

    String folderPath = ".";
    String version = MapGenerator.VERSION;
    BaseEncoding NameEncoder = BaseEncoding.base32().omitPadding().lowerCase();
    long seed = 1234;
    byte spawnCount = 2;
    float landDensity = StrictMath.round(.1f * 127f) / 127f;
    float plateauDensity = StrictMath.round(.1f / PLATEAU_DENSITY_MAX * 127f) / 127f * PLATEAU_DENSITY_MAX;
    float mountainDensity = StrictMath.round(.025f / MOUNTAIN_DENSITY_MAX * 127f) / 127f * MOUNTAIN_DENSITY_MAX;
    float rampDensity = StrictMath.round(.1f / RAMP_DENSITY_MAX * 127f) / 127f * RAMP_DENSITY_MAX;
    float reclaimDensity = StrictMath.round(.1f * 127f) / 127f;
    int mexCount = 16;
    String symmetry = "POINT";
    int mapSize = 512;
    byte[] optionArray = {spawnCount,
            (byte) (mapSize / 64),
            (byte) (landDensity * 127f),
            (byte) (plateauDensity / PLATEAU_DENSITY_MAX * 127f),
            (byte) (mountainDensity / MOUNTAIN_DENSITY_MAX * 127f),
            (byte) (rampDensity / RAMP_DENSITY_MAX * 127f),
            (byte) (reclaimDensity * 127f),
            (byte) (mexCount),
            (byte) (Symmetry.valueOf(symmetry).ordinal())};
    byte[] clientOptionArray = {spawnCount,
            (byte) (mapSize / 64),
            (byte) (landDensity * 127f),
            (byte) (plateauDensity / PLATEAU_DENSITY_MAX * 127f),
            (byte) (mountainDensity / MOUNTAIN_DENSITY_MAX * 127f),
            (byte) (rampDensity / RAMP_DENSITY_MAX * 127f)};
    ByteBuffer seedBuffer = ByteBuffer.allocate(8).putLong(seed);
    String numericMapName = String.format("neroxis_map_generator_%s_%d", version, seed);
    String b32MapName = String.format("neroxis_map_generator_%s_%s_%s", version, NameEncoder.encode(seedBuffer.array()),NameEncoder.encode(optionArray));
    String b32MapNameClient = String.format("neroxis_map_generator_%s_%s_%s", version, NameEncoder.encode(seedBuffer.array()),NameEncoder.encode(clientOptionArray));
    String[] keywordArgs = {"--folder-path", ".",
            "--seed", Long.toString(seed),
            "--spawn-count", Byte.toString(spawnCount),
            "--land-density", Float.toString(landDensity),
            "--plateau-density", Float.toString(plateauDensity),
            "--mountain-density", Float.toString(mountainDensity),
            "--ramp-density", Float.toString(rampDensity),
            "--reclaim-density", Float.toString(reclaimDensity),
            "--mex-count", Integer.toString(mexCount),
            "--symmetry", symmetry,
            "--map-size", Integer.toString(mapSize)};
    private MapGenerator instance;

    @Before
    public void setup() {
        instance = new MapGenerator();
    }

    @Test
    public void TestParseOrdered3Args() {
        String[] args = {folderPath, Long.toString(seed), version};

        instance.interpretArguments(args);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getFolderPath(), folderPath);
    }

    @Test
    public void TestParseOrdered2Args() {
        String[] args = {folderPath, numericMapName};

        instance.interpretArguments(args);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getFolderPath(), folderPath);
    }

    @Test
    public void TestParseOrdered4Args() {
        String[] args = {folderPath, Long.toString(seed), version, numericMapName};

        instance.interpretArguments(args);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getFolderPath(), folderPath);
    }

    @Test
    public void TestParseB32MapName() {
        String[] args = {folderPath, b32MapName};

        instance.interpretArguments(args);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getFolderPath(), folderPath);
        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getFolderPath(), folderPath);
        assertEquals(instance.getLandDensity(), landDensity, .01);
        assertEquals(instance.getPlateauDensity(), plateauDensity, .01);
        assertEquals(instance.getMountainDensity(), mountainDensity, .01);
        assertEquals(instance.getRampDensity(), rampDensity, .01);
        assertEquals(instance.getReclaimDensity(), reclaimDensity, .01);
        assertEquals(instance.getMexCount(), mexCount);
        assertEquals(instance.getSymmetry(), Symmetry.valueOf(symmetry));
        assertEquals(instance.getMapSize(), mapSize);
    }

    @Test
    public void TestParseClientB32MapName() {
        String[] args = {folderPath, b32MapNameClient};

        instance.interpretArguments(args);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getFolderPath(), folderPath);
        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getFolderPath(), folderPath);
        assertEquals(instance.getLandDensity(), landDensity, .01);
        assertEquals(instance.getPlateauDensity(), plateauDensity, .01);
        assertEquals(instance.getMountainDensity(), mountainDensity, .01);
        assertEquals(instance.getRampDensity(), rampDensity, .01);
        assertEquals(instance.getMapSize(), mapSize);
    }

    @Test
    public void TestParseKeywordArgs() {
        instance.interpretArguments(keywordArgs);

        assertEquals(instance.getSeed(), seed);
        assertEquals(instance.getFolderPath(), folderPath);
        assertEquals(instance.getLandDensity(), landDensity, .01);
        assertEquals(instance.getPlateauDensity(), plateauDensity, .01);
        assertEquals(instance.getMountainDensity(), mountainDensity, .01);
        assertEquals(instance.getRampDensity(), rampDensity, .01);
        assertEquals(instance.getReclaimDensity(), reclaimDensity, .01);
        assertEquals(instance.getMexCount(), mexCount);
        assertEquals(instance.getSymmetry(), Symmetry.valueOf(symmetry));
        assertEquals(instance.getMapSize(), mapSize);
    }

    @Test
    public void TestDeterminism() {
        instance.interpretArguments(keywordArgs);
        SCMap map1 = instance.generate();
        instance.save(instance.getFolderPath(), instance.getMapName(), map1);

        Pipeline.reset();

        instance.interpretArguments(keywordArgs);
        SCMap map2 = instance.generate();
        instance.save(instance.getFolderPath(), instance.getMapName(), map2);

        assertArrayEquals(map1.getSpawns(), map2.getSpawns());
        assertArrayEquals(map1.getMexes(), map2.getMexes());
        assertArrayEquals(map1.getHydros(), map2.getHydros());
        assertEquals(map1.getUnits(), map2.getUnits());
        assertEquals(map1.getWrecks(), map2.getWrecks());
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
    public void TestEqualityMapNameKeyword() {
        instance.interpretArguments(keywordArgs);
        SCMap map1 = instance.generate();

        Pipeline.reset();

        String[] args = {folderPath, b32MapName};
        instance.interpretArguments(args);
        SCMap map2 = instance.generate();

        assertArrayEquals(map1.getSpawns(), map2.getSpawns());
        assertArrayEquals(map1.getMexes(), map2.getMexes());
        assertArrayEquals(map1.getHydros(), map2.getHydros());
        assertEquals(map1.getUnits(), map2.getUnits());
        assertEquals(map1.getWrecks(), map2.getWrecks());
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
    public void TestEqualityMapNameNameKeyword() {
        String[] args;
        args = new String[]{"--map-name", b32MapName};
        instance.interpretArguments(args);
        SCMap map1 = instance.generate();

        Pipeline.reset();

        args = new String[]{folderPath, b32MapName};
        instance.interpretArguments(args);
        SCMap map2 = instance.generate();

        assertArrayEquals(map1.getSpawns(), map2.getSpawns());
        assertArrayEquals(map1.getMexes(), map2.getMexes());
        assertArrayEquals(map1.getHydros(), map2.getHydros());
        assertEquals(map1.getUnits(), map2.getUnits());
        assertEquals(map1.getWrecks(), map2.getWrecks());
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

    @After
    public void cleanup() {
        Pipeline.reset();
        FileUtils.deleteRecursiveIfExists(Paths.get(instance.getMapName()));
    }

}


