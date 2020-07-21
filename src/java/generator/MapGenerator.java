package generator;

import biomes.Biome;
import biomes.Biomes;
import com.google.common.io.BaseEncoding;
import export.SCMapExporter;
import export.SaveExporter;
import export.ScenarioExporter;
import export.ScriptExporter;
import lombok.Getter;
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

@Getter
public strictfp class MapGenerator {

    public static final boolean DEBUG = false;
    public static final String VERSION = "1.0.8";
    public static final BaseEncoding NAME_ENCODER = BaseEncoding.base32().omitPadding();

    public static final float MOUNTAIN_DENSITY_MAX = .075f;
    public static final float RAMP_DENSITY_MAX = .25f;

    //read from cli args
    private String folderPath = ".";
    private String mapName = "debugMap";
    private long seed = new Random().nextLong();
    private Random random = new Random(seed);

    //read from key value arguments or map name
    private int spawnCount = 6;
    private float landDensity;
    private float plateauDensity;
    private float mountainDensity;
    private float rampDensity;
    private int mapSize = 512;
    private float reclaimDensity;
    private int mexCount;
    private Symmetry symmetry;

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
    private ConcurrentBinaryMask spawnLandMask;
    private ConcurrentBinaryMask spawnPlateauMask;
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

    private SymmetryHierarchy symmetryHierarchy;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);
        if (DEBUG) {
            Path debugDir = Paths.get(".", "debug");
            FileUtils.deleteRecursiveIfExists(debugDir);
            Files.createDirectory(debugDir);
        }

        MapGenerator generator = new MapGenerator();

        generator.interpretArguments(args);

        System.out.println("Generating map " + generator.mapName.replace('/', '^'));
        SCMap map = generator.generate();
        System.out.println("Saving map to " + Paths.get(generator.folderPath).toAbsolutePath() + File.separator + generator.mapName.replace('/', '^'));
        System.out.println("Seed: " + generator.seed);
        System.out.println("Land Density: " + generator.landDensity);
        System.out.println("Plateau Density: " + generator.plateauDensity);
        System.out.println("Mountain Density: " + generator.mountainDensity);
        System.out.println("Ramp Density: " + generator.rampDensity);
        System.out.println("Reclaim Density: " + generator.reclaimDensity);
        System.out.println("Mex Count: " + generator.mexCount);
        System.out.println("Terrain Symmetry: " + generator.symmetry);
        System.out.println("Team Symmetry: " + generator.symmetryHierarchy.getTeamSymmetry());
        System.out.println("Spawn Symmetry: " + generator.symmetryHierarchy.getSpawnSymmetry());
        System.out.println("Spawn Separation: " + generator.spawnSeparation);
        generator.save(generator.folderPath, generator.mapName.replace('/', '^'), map);
        System.out.println("Done");

        generator.generateDebugOutput();
    }

    public void interpretArguments(String[] args) {
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

    private void interpretArguments(Map<String, String> arguments) {
        if (arguments.containsKey("help")) {
            System.out.println("map-gen usage:\n" +
                    "--help                 produce help message\n" +
                    "--folder-path arg      optional, set the target folder for the generated map\n" +
                    "--seed arg             optional, set the seed for the generated map\n" +
                    "--map-name arg         optional, set the map name for the generated map\n" +
                    "--spawn-count arg      optional, set the spawn count for the generated map\n" +
                    "--land-density arg     optional, set the land density for the generated map\n" +
                    "--plateau-density arg  optional, set the plateau density for the generated map\n" +
      String.format("--mountain-density arg optional, set the mountain density for the generated map (max %.2f)\n", MOUNTAIN_DENSITY_MAX) +
      String.format("--ramp-density arg     optional, set the ramp density for the generated map (max %.2f)\n", RAMP_DENSITY_MAX) +
                    "--reclaim-density arg  optional, set the reclaim density for the generated map\n" +
                    "--mex-count arg        optional, set the mex count per player for the generated map\n" +
                    "--symmetry arg         optional, set the symmetry for the generated map (Point, X, Y, XY, YX)\n" +
                    "--map-size arg		    optional, set the map size (5km = 256, 10km = 512, 20km = 1024)");
            System.exit(0);
        }

        if (arguments.containsKey("folder-path")) {
            folderPath = arguments.get("folder-path");
        }

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
            plateauDensity = Float.parseFloat(arguments.get("plateau-density"));
            plateauDensity = (float) StrictMath.round(plateauDensity * 127f) / 127f;
        }

        if (arguments.containsKey("mountain-density")) {
            mountainDensity = StrictMath.min(Float.parseFloat(arguments.get("mountain-density")), MOUNTAIN_DENSITY_MAX);
            mountainDensity = (float) StrictMath.round(mountainDensity * 127f) / 127f;
        }

        if (arguments.containsKey("ramp-density")) {
            rampDensity = StrictMath.min(Float.parseFloat(arguments.get("ramp-density")), RAMP_DENSITY_MAX);
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

    private void parseMapName() {
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
                byte[] seedBytes = NAME_ENCODER.decode(seedString);
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
            byte[] optionBytes = NAME_ENCODER.decode(optionString);
            parseOptions(optionBytes);
        }
    }

    private void randomizeOptions() {
        landDensity = (random.nextInt(127 - 32) + 32)/ 127f;
        plateauDensity = (random.nextInt(127 - 32) + 16)/ 127f;
        mountainDensity = random.nextInt(127) / 127f * MOUNTAIN_DENSITY_MAX;
        rampDensity = (random.nextInt(127 - 32) + 32) / 127f * RAMP_DENSITY_MAX;
        reclaimDensity = random.nextInt(127) / 127f;
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

    private void parseOptions(byte[] optionBytes) {
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
            landDensity = optionBytes[2] / 127f;
        }
        if (optionBytes.length > 3) {
            plateauDensity = (float) optionBytes[3] / 127f;
        }
        if (optionBytes.length > 4) {
            mountainDensity = (float) optionBytes[4] / 127f * MOUNTAIN_DENSITY_MAX;
        }
        if (optionBytes.length > 5) {
            rampDensity = (float) optionBytes[5] / 127f * RAMP_DENSITY_MAX;
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

    private void generateMapName() {
        String mapNameFormat = "neroxis_map_generator_%s_%s_%s";
        ByteBuffer seedBuffer = ByteBuffer.allocate(8);
        seedBuffer.putLong(seed);
        String seedString = NAME_ENCODER.encode(seedBuffer.array());
        byte[] optionArray = {(byte) spawnCount,
                (byte) (mapSize / 64),
                (byte) (landDensity * 127f),
                (byte) (plateauDensity * 127f),
                (byte) (mountainDensity / MOUNTAIN_DENSITY_MAX * 127f),
                (byte) (rampDensity / RAMP_DENSITY_MAX * 127f),
                (byte) (reclaimDensity * 127f),
                (byte) (mexCount),
                (byte) (symmetry.ordinal())};
        String optionString = NAME_ENCODER.encode(optionArray);
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

        spawnSeparation = StrictMath.max(random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16, 24);

        BinaryMask[] spawnMasks = markerGenerator.generateSpawns(spawnSeparation, symmetry, plateauDensity);
        spawnLandMask = new ConcurrentBinaryMask(spawnMasks[0], random.nextLong(), "spawnsLand");
        spawnPlateauMask = new ConcurrentBinaryMask(spawnMasks[1], random.nextLong(), "spawnsPlateau");

        symmetryHierarchy = spawnLandMask.getBinaryMask().getSymmetryHierarchy();
        setupTerrainPipeline();
        setupHeightmapPipeline();
        setupTexturePipeline();
        setupWreckPipeline();
        setupPropPipeline();
        setupResourcePipeline();

        Pipeline.start();

        Pipeline.await(resourceMask, plateaus, land, rock);

        CompletableFuture<Void> ResourcesFuture = CompletableFuture.runAsync(() -> {
            long sTime = System.currentTimeMillis();
            BinaryMask plateauResource = new BinaryMask(resourceMask.getBinaryMask(), random.nextLong());
            plateauResource.intersect(plateaus.getBinaryMask()).trimEdge(16).fillCenter(16, true);
            BinaryMask waterMex = new BinaryMask(land.getBinaryMask(), random.nextLong());
            waterMex.invert().deflate(48).trimEdge(16).fillCenter(16, false);
            markerGenerator.generateMexes(resourceMask.getBinaryMask(), plateauResource, waterMex);
            BinaryMask hydroSpawn = new BinaryMask(land.getBinaryMask(), random.nextLong());
            hydroSpawn.minus(ramps.getBinaryMask()).minus(rock.getBinaryMask()).deflate(6);
            markerGenerator.generateHydros(resourceMask.getBinaryMask().deflate(6), hydroSpawn);
            generateExclusionMasks();
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateResources\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        ResourcesFuture.join();

        CompletableFuture<Void> wrecksFuture = CompletableFuture.runAsync(() -> {
            long sTime = System.currentTimeMillis();
            wreckGenerator.generateWrecks(t1LandWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T1_Land, 2f);
            wreckGenerator.generateWrecks(t2LandWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T2_Land, 15f);
            wreckGenerator.generateWrecks(t3LandWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T3_Land, 30f);
            wreckGenerator.generateWrecks(t2NavyWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T2_Navy, 96f);
            wreckGenerator.generateWrecks(navyFactoryWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.Navy_Factory, 256f);
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
        land = new ConcurrentBinaryMask(64, random.nextLong(), symmetryHierarchy, "land");
        mountains = new ConcurrentBinaryMask(32, random.nextLong(), symmetryHierarchy, "mountains");
        plateaus = new ConcurrentBinaryMask(32, random.nextLong(), symmetryHierarchy, "plateaus");
        ramps = new ConcurrentBinaryMask(64, random.nextLong(), symmetryHierarchy, "ramps");


        land.randomize(landDensity).acid(.8f).inflate(1).enlarge(128).smooth(8f).acid(.5f).inflate(1).deflate(1);
        mountains.randomize(mountainDensity).inflate(1).cutCorners().acid(.5f).enlarge(128).smooth(4).acid(.5f);
        plateaus.randomize(plateauDensity).acid(.8f).inflate(1).enlarge(128).smooth(8f).acid(.5f).inflate(1).deflate(1);

        plateaus.intersect(land).minus(mountains);
        land.combine(mountains);

        land.enlarge(mapSize + 1).smooth(8);
        mountains.enlarge(mapSize + 1).smooth(8);
        plateaus.enlarge(mapSize + 1).smooth(8);

        spawnLandMask.shrink(32).inflate(1).cutCorners().acid(.5f, symmetryHierarchy.getSpawnSymmetry()).enlarge(128).inflate(2).cutCorners();
        spawnLandMask.acid(.5f, symmetryHierarchy.getSpawnSymmetry()).enlarge(mapSize + 1).inflate(8);
        spawnPlateauMask.shrink(32).inflate(2).cutCorners().acid(.5f, symmetryHierarchy.getSpawnSymmetry());
        spawnPlateauMask.enlarge(128).inflate(2).cutCorners().acid(.5f, symmetryHierarchy.getSpawnSymmetry()).enlarge(mapSize + 1).smooth(8);
        spawnPlateauMask.deflate(8).intersect(spawnLandMask);

        plateaus.minus(spawnLandMask).combine(spawnPlateauMask).inflate(4).deflate(4);
        land.combine(spawnLandMask).combine(spawnPlateauMask).inflate(4).deflate(4);
        mountains.minus(spawnLandMask);

        ramps.randomize(rampDensity);
        ramps.intersect(plateaus).outline().minus(plateaus).minus(mountains).inflate(8);

        land.combine(ramps);
        mountains.minus(ramps);
    }

    private void setupHeightmapPipeline() {
        heightmapBase = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), "heightmapBase");
        ConcurrentFloatMask heightmapLand = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), "heightmapLand");
        ConcurrentFloatMask heightmapMountains = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), "heightmapMountains");
        ConcurrentFloatMask heightmapPlateaus = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), "heightmapPlateaus");

        plateaus.combine(mountains);
        heightmapBase.init(land, 25f, 25f);
        heightmapPlateaus.init(plateaus, 0, 3f).smooth(5f, ramps);
        heightmapLand.maskToHeightmap(0.025f, 0.25f, 48, land).smooth(2);
        heightmapMountains.maskToMoutains(2f, 0.5f, mountains);
        heightmapMountains.add(heightmapPlateaus).smooth(1);

        heightmapBase.add(heightmapLand);
        heightmapBase.add(heightmapMountains);
    }

    private void setupTexturePipeline() {
        grass = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetryHierarchy, "grass");
        rock = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetryHierarchy, "rock");
        ConcurrentBinaryMask lightGrass = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetryHierarchy, "lightGrass");
        ConcurrentBinaryMask plateauCopy = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetryHierarchy, "plateauCopy");
        rockTexture = new ConcurrentFloatMask(mapSize / 2, random.nextLong(), "rockTexture");
        grassTexture = new ConcurrentFloatMask(mapSize / 2, random.nextLong(), "grassTexture");
        lightGrassTexture = new ConcurrentFloatMask(mapSize / 2, random.nextLong(), "lightGrassTexture");

        plateauCopy.combine(plateaus).outline().minus(ramps);
        rock.combine(mountains).combine(plateauCopy).inflate(3).shrink(mapSize / 2);
        grass.combine(land).deflate(6f).combine(plateaus).shrink(mapSize / 2).inflate(1);
        lightGrass.randomize(.5f).shrink(mapSize / 2);

        rockTexture.init(rock, 0, .999f).smooth(1);
        grassTexture.init(grass, 0, .999f).smooth(2);
        lightGrassTexture.init(lightGrass, 0, .999f).smooth(4);
    }

    private void setupWreckPipeline() {
        t1LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetryHierarchy, "t1LandWreck");
        t2LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetryHierarchy, "t2LandWreck");
        t3LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetryHierarchy, "t3LandWreck");
        t2NavyWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetryHierarchy, "t2NavyWreck");
        navyFactoryWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetryHierarchy, "navyFactoryWreck");
        allWreckMask = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetryHierarchy, "allWreck");
        ConcurrentBinaryMask landCopy = new ConcurrentBinaryMask(land, random.nextLong(), "landCopy");

        t1LandWreckMask.randomize(reclaimDensity * .015f).intersect(land).deflate(mapSize / 512f).trimEdge(20);
        t2LandWreckMask.randomize(reclaimDensity * .02f).intersect(land).minus(t1LandWreckMask).trimEdge(64);
        t3LandWreckMask.randomize(reclaimDensity * .002f).intersect(land).minus(t1LandWreckMask).minus(t2LandWreckMask).trimEdge(mapSize / 8);
        navyFactoryWreckMask.randomize(reclaimDensity * .005f).minus(landCopy.inflate(8)).trimEdge(20);
        t2NavyWreckMask.randomize(reclaimDensity * .015f).intersect(landCopy.deflate(9).outline()).trimEdge(20);
        allWreckMask.combine(t1LandWreckMask).combine(t2LandWreckMask).combine(t3LandWreckMask).combine(t2NavyWreckMask).inflate(2);
    }

    private void setupPropPipeline() {
        treeMask = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetryHierarchy, "tree");
        cliffRockMask = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetryHierarchy, "cliffRock");
        fieldStoneMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetryHierarchy, "fieldStone");
        rockFieldMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetryHierarchy, "rockField");

        cliffRockMask.randomize(.3f).intersect(rock).minus(plateaus).minus(mountains).intersect(land).inflate(2);
        fieldStoneMask.randomize(reclaimDensity * .005f).enlarge(256).intersect(grass);
        fieldStoneMask.enlarge(mapSize + 1).trimEdge(10);
        treeMask.randomize(.1f).inflate(1).cutCorners().acid(.5f).enlarge(mapSize / 4).smooth(4).acid(.5f);
        treeMask.enlarge(mapSize / 2).intersect(grass).minus(rock);
        treeMask.enlarge(mapSize + 1).deflate(5).trimEdge(3).fillCircle(mapSize / 2f, mapSize / 2f, mapSize / 8f, false);
        rockFieldMask.randomize(reclaimDensity * .0005f).trimEdge(mapSize / 32).inflate(3).acid(.5f).intersect(land).minus(mountains);
    }

    private void setupResourcePipeline() {
        resourceMask = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetryHierarchy, "resource");

        resourceMask.combine(grass).minus(rock).minus(ramps).deflate(4);
        resourceMask.trimEdge(16).fillCenter(16, false);
    }

    private void generateExclusionMasks() {
        noProps = new BinaryMask(mapSize / 2, random.nextLong(), symmetryHierarchy);
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

        noWrecks = new BinaryMask(mapSize / 2, random.nextLong(), symmetryHierarchy);
        noWrecks.combine(rock.getBinaryMask());
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

    public String hashFiles(Path... files) throws NoSuchAlgorithmException {
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
