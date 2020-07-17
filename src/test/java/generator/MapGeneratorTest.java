package generator;

import map.SCMap;
import map.Symmetry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.FileUtils;
import util.Pipeline;

import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MapGeneratorTest {

    private MapGenerator instance;
    String folderPath = ".";
    String version = MapGenerator.VERSION;
    long seed = 1234;
    byte spawnCount = 2;
    float landDensity = StrictMath.round(.1f * 127f) / 127f;
    float plateauDensity = StrictMath.round(.1f * 127f) / 127f;
    float mountainDensity = StrictMath.round(.05f * 127f) / 127f;
    float rampDensity = StrictMath.round(.1f * 127f) / 127f;
    float reclaimDensity = StrictMath.round(.1f * 127f) / 127f;
    int mexCount = 16;
    String symmetry = "POINT";
    int mapSize = 512;
    byte[] optionArray = {spawnCount,
            (byte) (mapSize / 64),
            (byte) (landDensity * 127f),
            (byte) (plateauDensity / .2f * 127f),
            (byte) (mountainDensity / .075f * 127f),
            (byte) (rampDensity / .2f * 127f),
            (byte) (reclaimDensity * 127f),
            (byte) (mexCount),
            (byte) (Symmetry.valueOf(symmetry).ordinal())};
    ByteBuffer seedBuffer = ByteBuffer.allocate(8).putLong(seed);
    String numericMapName = String.format("neroxis_map_generator_%s_%d", version, seed);
    String b64MapName = String.format("neroxis_map_generator_%s_%s_%s", version, Base64.getEncoder().encodeToString(seedBuffer.array()), Base64.getEncoder().encodeToString(optionArray));
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
    public void TestParseB64MapName() {
        String[] args = {folderPath, b64MapName};

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
        instance.save(folderPath,"map1", map1);

        Pipeline.reset();

        instance.interpretArguments(keywordArgs);
        SCMap map2 = instance.generate();
        instance.save(folderPath,"map2", map2);

        assertTrue(map1.equals(map2));
    }

    @After
    public void cleanup() {
        FileUtils.deleteRecursiveIfExists(Paths.get(instance.getMapName()));
    }

}


