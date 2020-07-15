package generator;

import biomes.Biome;
import biomes.Biomes;
import export.SCMapExporter;
import export.SaveExporter;
import export.ScenarioExporter;
import export.ScriptExporter;
import lombok.SneakyThrows;
import map.*;
import util.ArgumentParser;
import util.FileUtils;
import util.Pipeline;
import util.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public strictfp class MapGenerator {

    public static final boolean DEBUG = true;
    public static final String VERSION = "1.0.5";

    //read from cli args
    private static String folderPath = ".";
    private static String mapName = "debugMap";
    private static long seed = new Random().nextLong();
    private static Random random = new Random(seed);

    //read from key value arguments or map name
    private static int spawnCount = 6;
    private static float landDensity;
    private static float plateauDensity;
    private static float mountainDensity;
    private static float rampDensity;
    private static int mapSize = 512;
    private static float reclaimDensity;
    private static int mexCount;
    private static Symmetry symmetry;

    private SCMap map;
    private int spawnSeparation;

    //masks used in generation
    private ConcurrentBinaryMask land;
    private ConcurrentBinaryMask mountains;
    private ConcurrentBinaryMask plateaus;
    private ConcurrentBinaryMask ramps;
    private ConcurrentFloatMask heightmapBase;
    private ConcurrentBinaryMask grass;
    private ConcurrentFloatMask grassTexture;
    private ConcurrentBinaryMask rock;
    private ConcurrentFloatMask rockTexture;
    private ConcurrentBinaryMask allWreckMask;
    private ConcurrentBinaryMask spawnsMask;
    private ConcurrentBinaryMask resourceMask;
    private ConcurrentFloatMask lightGrassTexture;
    private ConcurrentBinaryMask t1LandWreckMask;
    private ConcurrentBinaryMask t2LandWreckMask;
    private ConcurrentBinaryMask t3LandWreckMask;
    private ConcurrentBinaryMask t2NavyWreckMask;
    private ConcurrentBinaryMask navyFactoryWreckMask;
    private ConcurrentBinaryMask treeMask;
    private ConcurrentBinaryMask cliffRockMask;
    private ConcurrentBinaryMask fieldStoneMask;
    private ConcurrentBinaryMask rockFieldMask;
    private BinaryMask noProps;
    private BinaryMask noWrecks;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);
        if (DEBUG) {
            Path debugDir = Paths.get(".", "debug");
            FileUtils.deleteRecursiveIfExists(debugDir);
            Files.createDirectory(debugDir);
        }

        interpretArguments(args);

        MapGenerator generator = new MapGenerator();
        System.out.println("Generating map " + mapName.replace('/', '^'));
        SCMap map = generator.generate();
        System.out.println("Saving map to " + Paths.get(folderPath).toAbsolutePath() + File.separator + mapName.replace('/', '^'));
        System.out.println("Seed: " + seed);
        System.out.println("Land Density: " + landDensity);
        System.out.println("Plateau Density: " + plateauDensity);
        System.out.println("Mountain Density: " + mountainDensity);
        System.out.println("Ramp Density: " + rampDensity);
        System.out.println("Reclaim Density: " + reclaimDensity);
        System.out.println("Mex Count: " + mexCount);
        System.out.println("Terrain Symmetry: " + symmetry);
        System.out.println("Team Symmetry: " + generator.spawnsMask.getBinaryMask().getSymmetryHierarchy().getTeamSymmetry());
        System.out.println("Spawn Symmetry: " + generator.spawnsMask.getBinaryMask().getSymmetryHierarchy().getSpawnSymmetry());
        System.out.println("Spawn Separation: " + generator.spawnSeparation);
        generator.save(folderPath, mapName.replace('/', '^'), map);
        System.out.println("Done");

        generator.generateDebugOutput();
    }

    private static void interpretArguments(String[] args) {
        if (args.length == 0 || args[0].startsWith("--")) {
            interpretArguments(ArgumentParser.parse(args));
        } else if (args.length == 2) {
            folderPath = args[0];
            mapName = args[1];
            parseMapName();
        } else {
            try {
                folderPath = args[0];
                try {
                    seed = Long.parseLong(args[1]);
                    random = new Random(seed);
                } catch (NumberFormatException nfe) {
                    System.out.println("Seed not numeric using default seed or mapname");
                }
                if (!VERSION.equals(args[2])) {
                    System.out.println("This generator only supports version " + VERSION);
                    System.exit(-1);
                }
                if (args.length >= 4) {
                    mapName = args[3];
                    parseMapName();
                } else {
                    randomizeOptions();
                    generateMapName();
                }
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                System.out.println("Usage: generator [targetFolder] [seed] [expectedVersion] (mapName)");
            }
        }
    }

    private static void interpretArguments(Map<String, String> arguments) {
        if (arguments.containsKey("help")) {
            System.out.println("map-gen usage:\n" +
                    "--help                               produce help message\n" +
                    "--folder-path arg                    mandatory, set the target folder for the generated map\n" +
                    "--seed arg                           optional, set the seed for the generated map\n" +
                    "--map-name arg                       optional, set the map name for the generated map\n" +
                    "--spawn-count arg                    optional, set the spawn count for the generated map\n" +
                    "--land-density arg                   optional, set the land density for the generated map\n" +
                    "--plateau-density arg                optional, set the plateau density for the generated map (max .2)\n" +
                    "--mountain-density arg               optional, set the mountain density for the generated map (max .1)\n" +
                    "--ramp-density arg                   optional, set the ramp density for the generated map (max .2)\n" +
                    "--reclaim-density arg                optional, set the reclaim density for the generated map\n" +
                    "--mex-count arg                      optional, set the mex count per player for the generated map\n" +
                    "--symmetry arg                       optional, set the symmetry for the generated map (Point, X, Y, XY, YX)\n" +
                    "--map-size arg						  optional, set the map size (5km = 256, 10km = 512, 20km = 1024)");
            System.exit(0);
        }

        if (!arguments.containsKey("folder-path")) {
            System.out.println("Missing necessary argument.");
            System.exit(-1);
        }

        folderPath = arguments.get("folder-path");

        if (arguments.containsKey("map-name")) {
            mapName = arguments.get("map-name");
            parseMapName();
            return;
        }

        if (arguments.containsKey("seed")) {
            seed = Long.parseLong(arguments.get("seed"));
            random = new Random(seed);
        }

        if (arguments.containsKey("spawn-count")) {
            spawnCount = Integer.parseInt(arguments.get("spawn-count"));
        }

        if (arguments.containsKey("map-size")) {
            mapSize = Integer.parseInt(arguments.get("map-size"));
        }

        randomizeOptions();

        if (arguments.containsKey("land-density")) {
            landDensity = Float.parseFloat(arguments.get("land-density"));
            landDensity = (float) StrictMath.round(landDensity * 127f) / 127f;
        }

        if (arguments.containsKey("plateau-density")) {
            plateauDensity = StrictMath.min(Float.parseFloat(arguments.get("plateau-density")), .2f);
            plateauDensity = (float) StrictMath.round(plateauDensity * 127f) / 127f;
        }

        if (arguments.containsKey("mountain-density")) {
            mountainDensity = StrictMath.min(Float.parseFloat(arguments.get("mountain-density")), .1f);
            mountainDensity = (float) StrictMath.round(mountainDensity * 127f) / 127f;
        }

        if (arguments.containsKey("ramp-density")) {
            rampDensity = StrictMath.min(Float.parseFloat(arguments.get("ramp-density")), .2f);
            rampDensity = (float) StrictMath.round(rampDensity * 127f) / 127f;
        }

        if (arguments.containsKey("reclaim-density")) {
            reclaimDensity = Float.parseFloat(arguments.get("reclaim-density"));
            reclaimDensity = (float) StrictMath.round(reclaimDensity * 127f) / 127f;
        }

        if (arguments.containsKey("mex-count")) {
            mexCount = Integer.parseInt(arguments.get("mex-count"));
        }

        if (arguments.containsKey("symmetry")) {
            symmetry = Symmetry.valueOf(arguments.get("symmetry"));
        }

        if (arguments.containsKey("map-size")) {
            mapSize = Integer.parseInt(arguments.get("map-size"));
        }

        generateMapName();
    }

    private static void parseMapName() {
        mapName = mapName.replace('^', '/');
        if (!mapName.startsWith("neroxis_map_generator")) {
            throw new IllegalArgumentException("Map name is not a generated map");
        }
        String[] args = mapName.split("_");
        if (args.length < 4) {
            throw new RuntimeException("Version not specified");
        }
        String version = args[3];
        if (!VERSION.equals(version)) {
            throw new RuntimeException("Unsupported generator version: " + version);
        }
        if (args.length >= 5) {
            String seedString = args[4];
            try {
                seed = Long.parseLong(seedString);
            } catch (NumberFormatException nfe) {
                byte[] seedBytes = Base64.getDecoder().decode(seedString);
                ByteBuffer seedWrapper = ByteBuffer.wrap(seedBytes);
                seed = seedWrapper.getLong();
            }
            random = new Random(seed);
            if (args.length < 6) {
                randomizeOptions();
            }
        }
        if (args.length >= 6) {
            String optionString = args[5];
            byte[] optionBytes = Base64.getDecoder().decode(optionString);
            parseOptions(optionBytes);
        }
    }

    private static void randomizeOptions() {
        landDensity = StrictMath.max(random.nextInt(127), 13) / 127f * (512f / mapSize);
        plateauDensity = (float) random.nextInt(127) / 127f * .2f * mapSize / 512f;
        mountainDensity = (float) random.nextInt(127) / 127f * .075f * mapSize / 512f;
        rampDensity = (float) random.nextInt(127) / 127f * .2f * (512f / mapSize);
        reclaimDensity = (float) random.nextInt(127) / 127f;
        mexCount = (int) ((8 + 8 / spawnCount + random.nextInt(40 / spawnCount)) * (.5f + mapSize / 512f * .5f));
        Symmetry[] symmetries;
        if (spawnCount == 2) {
            symmetries = new Symmetry[]{Symmetry.POINT, Symmetry.QUAD, Symmetry.DIAG};
        } else {
            symmetries = Symmetry.values();
        }
        int symmetryValue = random.nextInt(symmetries.length);
        symmetry = symmetries[symmetryValue];
    }

    private static void parseOptions(byte[] optionBytes) {
        if (optionBytes.length > 0) {
            if (optionBytes[0] <= 16) {
                spawnCount = optionBytes[0];
            }
        }
        if (optionBytes.length > 1) {
            mapSize = (int) optionBytes[1] * 64;
        }

        randomizeOptions();

        if (optionBytes.length > 2) {
            landDensity = StrictMath.max(optionBytes[2] / 127f, 13.0f / 127f) * 512f / mapSize * 512f / mapSize;
        }
        if (optionBytes.length > 3) {
            plateauDensity = (float) optionBytes[3] / 127f * .2f;
        }
        if (optionBytes.length > 4) {
            mountainDensity = (float) optionBytes[4] / 127f * .075f;
        }
        if (optionBytes.length > 5) {
            rampDensity = (float) optionBytes[5] / 127f * .2f;
        }
        if (optionBytes.length > 6) {
            reclaimDensity = (float) optionBytes[6] / 127f;
        }
        if (optionBytes.length > 7) {
            mexCount = optionBytes[7];
        }
        if (optionBytes.length > 8) {
            symmetry = Symmetry.values()[optionBytes[8]];
        }

    }

    private static void generateMapName() {
        String mapNameFormat = "neroxis_map_generator_%s_%s_%s";
        ByteBuffer seedBuffer = ByteBuffer.allocate(8);
        seedBuffer.putLong(seed);
        String seedString = Base64.getEncoder().encodeToString(seedBuffer.array());
        byte[] optionArray = {(byte) spawnCount,
                (byte) (mapSize / 64),
                (byte) (landDensity * 127f / (512f / mapSize * 512f / mapSize)),
                (byte) (plateauDensity / .2f * 127f),
                (byte) (mountainDensity / .075f * 127f),
                (byte) (rampDensity / .2f * 127f),
                (byte) (reclaimDensity * 127f),
                (byte) (mexCount),
                (byte) (symmetry.ordinal())};
        String optionString = Base64.getEncoder().encodeToString(optionArray);
        mapName = String.format(mapNameFormat, VERSION, seedString, optionString);
    }

    public void save(String folderName, String mapName, SCMap map) {
        try {
            Path folderPath = Paths.get(folderName);

            FileUtils.deleteRecursiveIfExists(folderPath.resolve(mapName));

            Files.createDirectory(folderPath.resolve(mapName));
            SCMapExporter.exportSCMAP(folderPath, mapName, map);
            SaveExporter.exportSave(folderPath, mapName, map);
            ScenarioExporter.exportScenario(folderPath, mapName, map);
            ScriptExporter.exportScript(folderPath, mapName, map);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public SCMap generate() {
        long startTime = System.currentTimeMillis();

        final int hydroCount = spawnCount + random.nextInt(spawnCount / 2) * 2;
        map = new SCMap(mapSize, spawnCount, mexCount * spawnCount, hydroCount);

        MarkerGenerator markerGenerator = new MarkerGenerator(map, random.nextLong());
        WreckGenerator wreckGenerator = new WreckGenerator(map, random.nextLong());
        PropGenerator propGenerator = new PropGenerator(map, random.nextLong());

        setupTerrainPipeline();
        setupHeightmapPipeline();
        setupTexturePipeline();
        setupMarkerPipeline();
        setupWreckPipeline();
        setupPropPipeline();

        if (spawnCount == 2) {
            spawnSeparation = 192;
        } else {
            spawnSeparation = random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16;
        }
        Pipeline.start();

        Pipeline.await(spawnsMask, rock, ramps);

        CompletableFuture<Void> spawnAndResourcesFuture = CompletableFuture.runAsync(() -> {
            long sTime = System.currentTimeMillis();
            markerGenerator.generateSpawns(spawnsMask.getBinaryMask(), spawnSeparation);
            markerGenerator.generateMexes(resourceMask.getBinaryMask());
            markerGenerator.generateHydros(resourceMask.getBinaryMask());
            generateExclusionMasks();
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateSpawns/Resources\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        spawnAndResourcesFuture.join();

        CompletableFuture<Void> wrecksFuture = CompletableFuture.runAsync(() -> {
            long sTime = System.currentTimeMillis();
            wreckGenerator.generateWrecks(t1LandWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T1_Land, 2f);
            wreckGenerator.generateWrecks(t2LandWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T2_Land, 15f);
            wreckGenerator.generateWrecks(t3LandWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T3_Land, 30f);
            wreckGenerator.generateWrecks(t2NavyWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T2_Navy, 60f);
            wreckGenerator.generateWrecks(navyFactoryWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.Navy_Factory, 120f);
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateWrecks\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        CompletableFuture<Void> propsFuture = CompletableFuture.runAsync(() -> {
            long sTime = System.currentTimeMillis();
            propGenerator.generateProps(treeMask.getBinaryMask().minus(noProps), propGenerator.TREE_GROUPS, 3f);
            propGenerator.generateProps(cliffRockMask.getBinaryMask().minus(noProps), propGenerator.ROCKS, 1.5f);
            propGenerator.generateProps(rockFieldMask.getBinaryMask().minus(noProps), propGenerator.ROCKS, 2f);
            propGenerator.generateProps(fieldStoneMask.getBinaryMask().minus(noProps), propGenerator.FIELD_STONES, 60f);
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateProps\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        Pipeline.await(heightmapBase);
        wrecksFuture.join();
        propsFuture.join();

        CompletableFuture<Void> heightMapFuture = CompletableFuture.runAsync(() -> {
            long sTime = System.currentTimeMillis();
            map.setHeightmap(heightmapBase.getFloatMask());
            map.getHeightmap().getRaster().setPixel(0, 0, new int[]{0});
            markerGenerator.setMarkerHeights();
            propGenerator.setPropHeights();
            wreckGenerator.setWreckHeights();
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, setHeightmap\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        Pipeline.stop();
        heightMapFuture.join();

        map.setTextureMaskLow(grassTexture.getFloatMask(), lightGrassTexture.getFloatMask(), rockTexture.getFloatMask(), new FloatMask(mapSize + 1, 0));

        Biome biomeSet = Biomes.getRandomBiome(random);

        System.out.printf("Using biome %s\n", biomeSet.getName());
        map.biome.setTerrainMaterials(biomeSet.getTerrainMaterials());
        map.biome.setWaterSettings(biomeSet.getWaterSettings());
        map.biome.setLightingSettings(biomeSet.getLightingSettings());

        Preview.generate(map.getPreview(), map);

        System.out.printf("Map generation done: %d ms\n", System.currentTimeMillis() - startTime);

        return map;
    }

    private void setupTerrainPipeline() {
        land = new ConcurrentBinaryMask(mapSize / 32, random.nextLong(), symmetry, "land");
        mountains = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetry, "mountains");
        plateaus = new ConcurrentBinaryMask( mapSize / 16, random.nextLong(), symmetry, "plateaus");
        ramps = new ConcurrentBinaryMask(64, random.nextLong(), symmetry, "ramps");

        land.randomize(landDensity).inflate(1).cutCorners().enlarge(mapSize / 16).acid(.5f).enlarge(mapSize / 4).smooth(4).acid(.5f);
        mountains.randomize(mountainDensity).inflate(1).cutCorners().acid(.5f).enlarge(mapSize / 4).smooth(4).acid(.5f);
        plateaus.randomize(plateauDensity).inflate(1).cutCorners().acid(.5f).enlarge(mapSize / 4).smooth(4).acid(.5f);
        ramps.randomize(rampDensity);

        plateaus.intersect(land).minus(mountains);
        ramps.intersect(plateaus).outline().minus(plateaus).intersect(land).minus(mountains).inflate(2);
        land.combine(mountains);

        land.enlarge(mapSize + 1).smooth(6);
        mountains.enlarge(mapSize + 1).inflate(1).smooth(mapSize / 64f);
        plateaus.enlarge(mapSize + 1).inflate(1).smooth(mapSize / 64f);
        ramps.enlarge(mapSize + 1).smooth(6);
    }

    private void setupHeightmapPipeline() {
        heightmapBase = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), "heightmapBase");
        ConcurrentFloatMask heightmapLand = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), "heightmapLand");
        ConcurrentFloatMask heightmapMountains = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), "heightmapMountains");
        ConcurrentFloatMask heightmapPlateaus = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), "heightmapPlateaus");

        plateaus.combine(mountains);
        heightmapBase.init(land, 25f, 25f);
        heightmapPlateaus.init(plateaus, 0, 3f).smooth(5f, ramps);
        heightmapLand.maskToHeightmap(0.025f, 0.25f, 95, land).smooth(2);
        heightmapMountains.maskToMoutains(2f, 0.5f, mountains);
        heightmapMountains.add(heightmapPlateaus).smooth(1);

        heightmapBase.add(heightmapLand);
        heightmapBase.add(heightmapMountains);
    }

    private void setupTexturePipeline() {
        grass = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetry, "grass");
        rock = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetry, "rock");
        ConcurrentBinaryMask lightGrass = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetry, "lightGrass");
        ConcurrentBinaryMask plateauCopy = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetry, "plateauCopy");
        rockTexture = new ConcurrentFloatMask(mapSize / 2, random.nextLong(), "rockTexture");
        grassTexture = new ConcurrentFloatMask(mapSize / 2, random.nextLong(), "grassTexture");
        lightGrassTexture = new ConcurrentFloatMask(mapSize / 2, random.nextLong(), "lightGrassTexture");

        plateauCopy.combine(plateaus).outline().minus(ramps);
        rock.combine(mountains).combine(plateauCopy).inflate(3).shrink(mapSize / 2);
        grass.combine(land).deflate(6f).combine(plateaus).shrink(mapSize / 2).inflate(1);
        lightGrass.randomize(.2f).shrink(mapSize / 2);

        rockTexture.init(rock, 0, .999f).smooth(1);
        grassTexture.init(grass, 0, .999f).smooth(2);
        lightGrassTexture.init(lightGrass, 0, .999f).smooth(2);
    }

    private void setupMarkerPipeline() {
        spawnsMask = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetry, "spawns");
        if (spawnCount == 2 && (symmetry == Symmetry.POINT || symmetry == Symmetry.DIAG || symmetry == Symmetry.QUAD)) {
            spawnsMask.getBinaryMask().getSymmetryHierarchy().setSpawnSymmetry(Symmetry.POINT);
        }
        resourceMask = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy(), "resource");

        spawnsMask.combine(land).combine(plateaus).minus(rock).minus(ramps).deflate(18).trimEdge(32);
        resourceMask.combine(land).combine(plateaus).minus(rock).minus(ramps).deflate(8).trimEdge(32).fillCenter(16, false);
    }

    private void setupWreckPipeline() {
        t1LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy(), "t1LandWreck");
        t2LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy(), "t2LandWreck");
        t3LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy(), "t3LandWreck");
        t2NavyWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy(), "t2NavyWreck");
        navyFactoryWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy(), "navyFactoryWreck");
        allWreckMask = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy(), "allWreck");
        ConcurrentBinaryMask landCopy = new ConcurrentBinaryMask(land, random.nextLong(), "landCopy");

        t1LandWreckMask.randomize(reclaimDensity * .015f).intersect(land).deflate(mapSize / 512f).trimEdge(20);
        t2LandWreckMask.randomize(reclaimDensity * .01f).intersect(land).minus(t1LandWreckMask).trimEdge(20);
        t3LandWreckMask.randomize(reclaimDensity * .002f).intersect(land).minus(t1LandWreckMask).minus(t2LandWreckMask).trimEdge(mapSize / 8);
        t2NavyWreckMask.randomize(reclaimDensity * .015f).intersect(landCopy.outline()).trimEdge(20);
        navyFactoryWreckMask.randomize(reclaimDensity * .0075f).minus(land).deflate(6).trimEdge(20);
        allWreckMask.combine(t1LandWreckMask).combine(t2LandWreckMask).combine(t3LandWreckMask).combine(t2NavyWreckMask).inflate(2);
    }

    private void setupPropPipeline() {
        treeMask = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy(), "tree");
        cliffRockMask = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy(), "cliffRock");
        fieldStoneMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy(), "fieldStone");
        rockFieldMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy(), "rockField");

        cliffRockMask.randomize(.15f).intersect(rock).minus(plateaus).minus(mountains).intersect(land).inflate(2);
        fieldStoneMask.randomize(reclaimDensity * .005f).enlarge(256).intersect(grass);
        fieldStoneMask.enlarge(mapSize + 1).trimEdge(10);
        treeMask.randomize(.1f).inflate(1).cutCorners().acid(.5f).enlarge(mapSize / 4).smooth(4).acid(.5f);
        treeMask.enlarge(mapSize / 2).intersect(grass).minus(rock);
        treeMask.enlarge(mapSize + 1).deflate(5).trimEdge(3).fillCircle(mapSize / 2f, mapSize / 2f, mapSize / 4f, false);
        rockFieldMask.randomize(reclaimDensity * .005f).trimEdge(mapSize / 8).inflate(3).acid(.5f).intersect(land).minus(mountains);
    }

    private void generateExclusionMasks() {
        noProps = new BinaryMask(mapSize / 2, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy());
        noProps.combine(rock.getBinaryMask()).combine(ramps.getBinaryMask());
        for (int i = 0; i < map.getSpawns().length; i++) {
            noProps.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 30, true);
        }
        for (int i = 0; i < map.getMexes().length; i++) {
            if (map.getMexes()[i] != null) {
                noProps.fillCircle(map.getMexes()[i].x, map.getMexes()[i].z, 5, true);
            }
        }
        for (int i = 0; i < map.getHydros().length; i++) {
            if (map.getHydros()[i] != null) {
                noProps.fillCircle(map.getHydros()[i].x, map.getHydros()[i].z, 7, true);
            }
        }
        noProps.combine(allWreckMask.getBinaryMask());

        noWrecks = new BinaryMask(mapSize / 2, random.nextLong(), spawnsMask.getBinaryMask().getSymmetryHierarchy());
        noWrecks.combine(rock.getBinaryMask()).combine(ramps.getBinaryMask());
        for (int i = 0; i < map.getSpawns().length; i++) {
            noWrecks.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 96, true);
        }
        for (int i = 0; i < map.getMexes().length; i++) {
            if (map.getMexes()[i] != null) {
                noWrecks.fillCircle(map.getMexes()[i].x, map.getMexes()[i].z, 10, true);
            }
        }
        for (int i = 0; i < map.getHydros().length; i++) {
            if (map.getHydros()[i] != null) {
                noWrecks.fillCircle(map.getHydros()[i].x, map.getHydros()[i].z, 15, true);
            }
        }
    }

    @SneakyThrows({IOException.class, NoSuchAlgorithmException.class})
    private void generateDebugOutput() {
        if (!DEBUG) {
            return;
        }

        FileWriter writer = new FileWriter(Paths.get(".", "debug", "summary.txt").toFile());
        Path masksDir = Paths.get(".", "debug");

        for (int i = 0; i < Pipeline.getPipelineSize(); i++) {
            Path maskFile = masksDir.resolve(i + ".mask");
            writer.write(String.format("%d:\t%s\n", i, hashFiles(maskFile)));
        }

        String mapHash = hashFiles(SCMapExporter.file.toPath(), SaveExporter.file.toPath());
        System.out.println("Map hash: " + mapHash);
        writer.write(String.format("Map hash:\t%s", mapHash));
        writer.flush();
        writer.close();
    }

    private String hashFiles(Path... files) throws NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(files).map(this::hashFile).forEach(sb::append);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes());
        return toHex(hash);
    }

    @SneakyThrows({IOException.class, NoSuchAlgorithmException.class})
    private String hashFile(Path file) {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
        return toHex(hash);
    }

    private String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte datum : data) {
            sb.append(String.format("%02x", datum));
        }
        return sb.toString();
    }
}
