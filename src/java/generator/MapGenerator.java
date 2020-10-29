package generator;

import biomes.Biome;
import biomes.Biomes;
import com.google.common.io.BaseEncoding;
import exporter.SCMapExporter;
import exporter.SaveExporter;
import exporter.ScenarioExporter;
import exporter.ScriptExporter;
import lombok.Getter;
import lombok.Setter;
import map.*;
import util.ArgumentParser;
import util.FileUtils;
import util.Pipeline;
import util.Util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static util.ImageUtils.readImage;

@Getter
@Setter
public strictfp class MapGenerator {

    public static final String VERSION = "1.1.6";
    private static final String BLANK_PREVIEW = "/images/generatedMapIcon.png";
    public static final BaseEncoding NAME_ENCODER = BaseEncoding.base32().omitPadding().lowerCase();
    public static final float LAND_DENSITY_MIN = .8f;
    public static final float LAND_DENSITY_MAX = .95f;
    public static final float LAND_DENSITY_RANGE = LAND_DENSITY_MAX - LAND_DENSITY_MIN;
    public static final float MOUNTAIN_DENSITY_MIN = 0f;
    public static final float MOUNTAIN_DENSITY_MAX = 1f;
    public static final float MOUNTAIN_DENSITY_RANGE = MOUNTAIN_DENSITY_MAX - MOUNTAIN_DENSITY_MIN;
    public static final float RAMP_DENSITY_MIN = .030f;
    public static final float RAMP_DENSITY_MAX = .065f;
    public static final float RAMP_DENSITY_RANGE = RAMP_DENSITY_MAX - RAMP_DENSITY_MIN;
    public static final float PLATEAU_DENSITY_MIN = .35f;
    public static final float PLATEAU_DENSITY_MAX = .5f;
    public static final float PLATEAU_DENSITY_RANGE = PLATEAU_DENSITY_MAX - PLATEAU_DENSITY_MIN;
    public static final float PLATEAU_HEIGHT = 5f;
    public static final float VALLEY_HEIGHT = -3f;
    public static final float HILL_HEIGHT = 3f;
    public static boolean DEBUG = false;
    //read from cli args
    private String pathToFolder = ".";
    private String mapName = "debugMap";
    private long seed = new Random().nextLong();
    private Random random = new Random(seed);
    private boolean tournamentStyle = false;
    private boolean blind = false;
    private long generationTime;

    //read from key value arguments or map name
    private int spawnCount = 6;
    private float landDensity;
    private float plateauDensity;
    private float mountainDensity;
    private float rampDensity;
    private int mapSize = 512;
    private float reclaimDensity;
    private int mexCount;
    private Symmetry terrainSymmetry;
    private Biome biome;

    private SCMap map;
    private int spawnSeparation;
    private float waterHeight;

    //masks used in generation
    private ConcurrentBinaryMask land;
    private ConcurrentBinaryMask mountains;
    private ConcurrentBinaryMask hills;
    private ConcurrentBinaryMask valleys;
    private ConcurrentBinaryMask plateaus;
    private ConcurrentBinaryMask cliffs;
    private ConcurrentBinaryMask shore;
    private ConcurrentBinaryMask ramps;
    private ConcurrentBinaryMask impassable;
    private ConcurrentBinaryMask unbuildable;
    private ConcurrentBinaryMask passable;
    private ConcurrentBinaryMask passableLand;
    private ConcurrentBinaryMask passableWater;
    private ConcurrentFloatMask slope;
    private ConcurrentFloatMask heightmapBase;
    private ConcurrentFloatMask accentGroundTexture;
    private ConcurrentFloatMask waterBeachTexture;
    private ConcurrentFloatMask accentSlopesTexture;
    private ConcurrentFloatMask accentPlateauTexture;
    private ConcurrentFloatMask slopesTexture;
    private ConcurrentFloatMask steepHillsTexture;
    private ConcurrentFloatMask rockTexture;
    private ConcurrentFloatMask accentRockTexture;
    private ConcurrentBinaryMask rockDecal;
    private ConcurrentBinaryMask intDecal;
    private ConcurrentBinaryMask allWreckMask;
    private ConcurrentBinaryMask spawnLandMask;
    private ConcurrentBinaryMask spawnPlateauMask;
    private ConcurrentBinaryMask resourceMask;
    private ConcurrentBinaryMask plateauResourceMask;
    private ConcurrentBinaryMask waterResourceMask;
    private ConcurrentBinaryMask t1LandWreckMask;
    private ConcurrentBinaryMask t2LandWreckMask;
    private ConcurrentBinaryMask t3LandWreckMask;
    private ConcurrentBinaryMask t2NavyWreckMask;
    private ConcurrentBinaryMask navyFactoryWreckMask;
    private ConcurrentBinaryMask treeMask;
    private ConcurrentBinaryMask cliffRockMask;
    private ConcurrentBinaryMask fieldStoneMask;
    private ConcurrentBinaryMask largeRockFieldMask;
    private ConcurrentBinaryMask smallRockFieldMask;
    private ConcurrentBinaryMask baseMask;
    private ConcurrentBinaryMask civReclaimMask;
    private ConcurrentBinaryMask allBaseMask;
    private BinaryMask noProps;
    private BinaryMask noWrecks;
    private BinaryMask noDecals;
    private BinaryMask noBases;
    private BinaryMask noCivs;

    private SymmetrySettings symmetrySettings;
    private boolean hasCivilians;
    private boolean enemyCivilians;
    private float mexMultiplier = 1f;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);
        if (DEBUG) {
            Path debugDir = Paths.get(".", "debug");
            FileUtils.deleteRecursiveIfExists(debugDir);
            Files.createDirectory(debugDir);
        }

        MapGenerator generator = new MapGenerator();
        generator.generationTime = Instant.now().getEpochSecond();

        generator.interpretArguments(args);

        System.out.println("Generating map " + generator.mapName.replace('/', '^'));
        generator.generate();
        generator.save();
        System.out.println("Saving map to " + Paths.get(generator.pathToFolder).toAbsolutePath() + File.separator + generator.mapName.replace('/', '^'));
        System.out.println("Seed: " + generator.seed);
        System.out.println("Biome: " + generator.biome.getName());
        System.out.println("Land Density: " + generator.landDensity);
        System.out.println("Plateau Density: " + generator.plateauDensity);
        System.out.println("Mountain Density: " + generator.mountainDensity);
        System.out.println("Ramp Density: " + generator.rampDensity);
        System.out.println("Reclaim Density: " + generator.reclaimDensity);
        System.out.println("Mex Count: " + generator.mexCount);
        System.out.println("Terrain Symmetry: " + generator.terrainSymmetry);
        System.out.println("Team Symmetry: " + generator.symmetrySettings.getTeamSymmetry());
        System.out.println("Spawn Symmetry: " + generator.symmetrySettings.getSpawnSymmetry());
        System.out.println("Spawn Separation: " + generator.spawnSeparation);
        System.out.println("Done");
    }

    public void interpretArguments(String[] args) {
        if (args.length == 0 || args[0].startsWith("--")) {
            interpretArguments(ArgumentParser.parse(args));
        } else if (args.length == 2) {
            pathToFolder = args[0];
            mapName = args[1];
            parseMapName();
        } else {
            try {
                pathToFolder = args[0];
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
                    "--mountain-density arg optional, set the mountain density for the generated map\n" +
                    "--ramp-density arg     optional, set the ramp density for the generated map\n" +
                    "--reclaim-density arg  optional, set the reclaim density for the generated map\n" +
                    "--mex-count arg        optional, set the mex count per player for the generated map\n" +
                    "--symmetry arg         optional, set the symmetry for the generated map (Point, X, Z, XZ, ZX)\n" +
                    "--map-size arg		    optional, set the map size (5km = 256, 10km = 512, 20km = 1024)\n" +
                    "--biome arg		    optional, set the biome\n" +
                    "--tournament-style     optional, set map to tournament style which will remove the preview.png and add time of original generation to map\n" +
                    "--blind     optional, set map to tournament style which will remove the preview in the scmap and add time of original generation to map\n" +
                    "--debug                optional, turn on debugging options");
            System.exit(0);
        }

        if (arguments.containsKey("debug")) {
            DEBUG = true;
        }

        if (arguments.containsKey("folder-path")) {
            pathToFolder = arguments.get("folder-path");
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

        tournamentStyle = arguments.containsKey("tournament-style") || arguments.containsKey("blind");
        blind = arguments.containsKey("blind");

        if (arguments.containsKey("land-density")) {
            landDensity = Float.parseFloat(arguments.get("land-density")) * LAND_DENSITY_RANGE + LAND_DENSITY_MIN;
            landDensity = (float) StrictMath.round((landDensity - LAND_DENSITY_MIN) / (LAND_DENSITY_RANGE) * 127f) / 127f * LAND_DENSITY_RANGE + LAND_DENSITY_MIN;
        }

        if (arguments.containsKey("plateau-density")) {
            plateauDensity = Float.parseFloat(arguments.get("plateau-density")) * PLATEAU_DENSITY_RANGE + PLATEAU_DENSITY_MIN;
            plateauDensity = (float) StrictMath.round((plateauDensity - PLATEAU_DENSITY_MIN) / PLATEAU_DENSITY_RANGE * 127f) / 127f * PLATEAU_DENSITY_RANGE + PLATEAU_DENSITY_MIN;
        }

        if (arguments.containsKey("mountain-density")) {
            mountainDensity = Float.parseFloat(arguments.get("mountain-density")) * MOUNTAIN_DENSITY_RANGE + MOUNTAIN_DENSITY_MIN;
            mountainDensity = (float) StrictMath.round((mountainDensity - MOUNTAIN_DENSITY_MIN) / MOUNTAIN_DENSITY_RANGE * 127f) / 127f * MOUNTAIN_DENSITY_RANGE + MOUNTAIN_DENSITY_MIN;
        }

        if (arguments.containsKey("ramp-density")) {
            rampDensity = Float.parseFloat(arguments.get("ramp-density")) * RAMP_DENSITY_RANGE + RAMP_DENSITY_MIN;
            rampDensity = (float) StrictMath.round((rampDensity - RAMP_DENSITY_MIN) / RAMP_DENSITY_RANGE * 127f) / 127f * RAMP_DENSITY_RANGE + RAMP_DENSITY_MIN;
        }

        if (arguments.containsKey("reclaim-density")) {
            reclaimDensity = Float.parseFloat(arguments.get("reclaim-density"));
            reclaimDensity = (float) StrictMath.round(reclaimDensity * 127f) / 127f;
        }

        if (arguments.containsKey("mex-count")) {
            mexCount = Integer.parseInt(arguments.get("mex-count"));
        }

        if (arguments.containsKey("symmetry")) {
            terrainSymmetry = Symmetry.valueOf(arguments.get("symmetry"));
        }

        if (arguments.containsKey("map-size")) {
            mapSize = Integer.parseInt(arguments.get("map-size"));
        }

        if (arguments.containsKey("biome")) {
            biome = Biomes.getBiomeByName(arguments.get("biome"));
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
        if (args.length >= 7) {
            String parametersString = args[6];
            byte[] parameterBytes = NAME_ENCODER.decode(parametersString);
            parseParameters(parameterBytes);
        }
        if (args.length >= 8) {
            String timeString = args[7];
            generationTime = ByteBuffer.wrap(NAME_ENCODER.decode(timeString)).getLong();
        }
    }

    private void randomizeOptions() {
        landDensity = random.nextInt(127) / 127f * LAND_DENSITY_RANGE + LAND_DENSITY_MIN;
        plateauDensity = random.nextInt(127) / 127f * PLATEAU_DENSITY_RANGE + PLATEAU_DENSITY_MIN;
        mountainDensity = random.nextInt(127) / 127f * MOUNTAIN_DENSITY_RANGE + MOUNTAIN_DENSITY_MIN;
        rampDensity = random.nextInt(127) / 127f * RAMP_DENSITY_RANGE + RAMP_DENSITY_MIN;
        reclaimDensity = random.nextInt(127) / 127f;
        if (mapSize < 512) {
            mexMultiplier = .75f;
        } else if (mapSize > 512) {
            mexMultiplier = switch (spawnCount) {
                case 2 -> 2.5f;
                case 4, 6 -> 1.75f;
                case 8, 10 -> 1.45f;
                default -> 1f;
            };
        }
        mexCount = switch (spawnCount) {
            case 2 -> 10 + random.nextInt(15);
            case 4 -> 9 + random.nextInt(8);
            case 6 -> 8 + random.nextInt(5);
            case 8 -> 8 + random.nextInt(4);
            case 10 -> 8 + random.nextInt(3);
            case 12 -> 6 + random.nextInt(4);
            case 14 -> 6 + random.nextInt(3);
            case 16 -> 6 + random.nextInt(2);
            default -> 8 + random.nextInt(8);
        };
        mexCount *= mexMultiplier;
        Symmetry[] symmetries;
        if (spawnCount == 2) {
            symmetries = new Symmetry[]{Symmetry.POINT, Symmetry.QUAD, Symmetry.DIAG};
        } else {
            symmetries = Symmetry.values();
        }
        int symmetryValue = random.nextInt(symmetries.length - 1);
        terrainSymmetry = symmetries[symmetryValue];
        Symmetry spawnSymmetry;
        Symmetry teamSymmetry;
        Symmetry[] teams;
        switch (terrainSymmetry) {
            case POINT -> {
                spawnSymmetry = terrainSymmetry;
                teams = new Symmetry[]{Symmetry.X, Symmetry.Z, Symmetry.XZ, Symmetry.ZX};
                teamSymmetry = teams[random.nextInt(teams.length)];
            }
            case QUAD -> {
                spawnSymmetry = Symmetry.POINT;
                teams = new Symmetry[]{Symmetry.X, Symmetry.Z};
                teamSymmetry = teams[random.nextInt(teams.length)];
            }
            case DIAG -> {
                spawnSymmetry = Symmetry.POINT;
                teams = new Symmetry[]{Symmetry.XZ, Symmetry.ZX};
                teamSymmetry = teams[random.nextInt(teams.length)];
            }
            default -> {
                spawnSymmetry = terrainSymmetry;
                teamSymmetry = terrainSymmetry;
            }
        }
        if (spawnCount == 2 && (terrainSymmetry == Symmetry.POINT || terrainSymmetry == Symmetry.DIAG || terrainSymmetry == Symmetry.QUAD)) {
            symmetrySettings.setSpawnSymmetry(Symmetry.POINT);
        }
        symmetrySettings = new SymmetrySettings(terrainSymmetry, teamSymmetry, spawnSymmetry);
        biome = Biomes.getRandomBiome(random);
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
            landDensity = optionBytes[2] / 127f * LAND_DENSITY_RANGE + LAND_DENSITY_MIN;
        }
        if (optionBytes.length > 3) {
            plateauDensity = (float) optionBytes[3] / 127f * PLATEAU_DENSITY_RANGE + PLATEAU_DENSITY_MIN;
        }
        if (optionBytes.length > 4) {
            mountainDensity = (float) optionBytes[4] / 127f * MOUNTAIN_DENSITY_RANGE + MOUNTAIN_DENSITY_MIN;
        }
        if (optionBytes.length > 5) {
            rampDensity = (float) optionBytes[5] / 127f * RAMP_DENSITY_RANGE + RAMP_DENSITY_MIN;
        }
        if (optionBytes.length > 6) {
            reclaimDensity = (float) optionBytes[6] / 127f;
        }
        if (optionBytes.length > 7) {
            mexCount = optionBytes[7];
        }
        if (optionBytes.length > 8) {
            terrainSymmetry = Symmetry.values()[optionBytes[8]];
        }
        if (optionBytes.length > 9) {
            biome = Biomes.list.get(optionBytes[9]);
        }
    }

    private void parseParameters(byte[] parameterBytes) {
        BitSet parameters = BitSet.valueOf(parameterBytes);
        tournamentStyle = parameters.get(0);
        blind = parameters.get(1);
    }

    private void generateMapName() {
        String mapNameFormat = "neroxis_map_generator_%s_%s_%s";
        ByteBuffer seedBuffer = ByteBuffer.allocate(8);
        seedBuffer.putLong(seed);
        String seedString = NAME_ENCODER.encode(seedBuffer.array());
        byte[] optionArray = {(byte) spawnCount,
                (byte) (mapSize / 64),
                (byte) ((landDensity - LAND_DENSITY_MIN) / LAND_DENSITY_RANGE * 127f),
                (byte) ((plateauDensity - PLATEAU_DENSITY_MIN) / PLATEAU_DENSITY_RANGE * 127f),
                (byte) ((mountainDensity - MOUNTAIN_DENSITY_MIN) / MOUNTAIN_DENSITY_RANGE * 127f),
                (byte) ((rampDensity - RAMP_DENSITY_MIN) / RAMP_DENSITY_RANGE * 127f),
                (byte) (reclaimDensity * 127f),
                (byte) (mexCount),
                (byte) (terrainSymmetry.ordinal()),
                (byte) (Biomes.list.indexOf(biome))};
        BitSet parameters = new BitSet();
        parameters.set(0, tournamentStyle);
        parameters.set(1, blind);
        String optionString = NAME_ENCODER.encode(optionArray) + "_" + NAME_ENCODER.encode(parameters.toByteArray());
        if (tournamentStyle) {
            String timeString = NAME_ENCODER.encode(ByteBuffer.allocate(8).putLong(generationTime).array());
            optionString += "_" + timeString;
        }
        mapName = String.format(mapNameFormat, VERSION, seedString, optionString);
    }

    public void save() {
        try {
            map.setName(mapName);
            Path folderPath = Paths.get(pathToFolder);

            FileUtils.deleteRecursiveIfExists(folderPath.resolve(mapName));

            long startTime = System.currentTimeMillis();
            Files.createDirectory(folderPath.resolve(mapName));
            SCMapExporter.exportSCMAP(folderPath.resolve(mapName), mapName, map);
            if (!tournamentStyle) {
                SCMapExporter.exportPreview(folderPath.resolve(mapName), mapName, map);
            }
            SaveExporter.exportSave(folderPath.resolve(mapName), mapName, map);
            ScenarioExporter.exportScenario(folderPath.resolve(mapName), mapName, map);
            ScriptExporter.exportScript(folderPath.resolve(mapName), mapName, map);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

            startTime = System.currentTimeMillis();
            Files.createDirectory(folderPath.resolve(mapName).resolve("debug"));
            SCMapExporter.exportSCMapString(folderPath, mapName, map);
            Pipeline.toFile(folderPath.resolve(mapName).resolve("debug").resolve("pipelineMaskHashes.txt"));
            toFile(folderPath.resolve(mapName).resolve("debug").resolve("generatorParams.txt"));
            System.out.printf("Debug export done: %d ms\n", System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public SCMap generate() throws IOException {
        long startTime = System.currentTimeMillis();

        final int spawnSize = 48;
        final int hydroCount = spawnCount + random.nextInt(spawnCount / 4) * 2;
        int mexSpacing = mapSize / 12;
        if (mapSize > 512) {
            landDensity = StrictMath.max(landDensity - .125f, .7f);
            mountainDensity = mountainDensity * .375f;
            mexSpacing = 64;
        }
        hasCivilians = random.nextBoolean();
        enemyCivilians = random.nextBoolean();
        map = new SCMap(mapSize, spawnCount, mexCount * spawnCount, hydroCount, biome);
        waterHeight = biome.getWaterSettings().getElevation();

        SpawnGenerator spawnGenerator = new SpawnGenerator(map, random.nextLong(), spawnSize);
        MexGenerator mexGenerator = new MexGenerator(map, random.nextLong(), spawnSize, mexSpacing);
        HydroGenerator hydroGenerator = new HydroGenerator(map, random.nextLong(), spawnSize);
        PropGenerator propGenerator = new PropGenerator(map, random.nextLong());
        DecalGenerator decalGenerator = new DecalGenerator(map, random.nextLong());
        UnitGenerator unitGenerator = new UnitGenerator(map, random.nextLong());
        AIMarkerGenerator aiMarkerGenerator = new AIMarkerGenerator(map, random.nextLong());

        spawnSeparation = switch (terrainSymmetry) {
            case Z, X -> StrictMath.max(StrictMath.max(random.nextInt(map.getSize() / 4 - map.getSize() / 32) + map.getSize() / 32, map.getSize() / spawnCount), 24);
            default -> StrictMath.max(random.nextInt(map.getSize() / 4 - map.getSize() / 32) + map.getSize() / 32, 24);
        };

        BinaryMask[] spawnMasks = spawnGenerator.generateSpawns(spawnSeparation, symmetrySettings, (plateauDensity - PLATEAU_DENSITY_MIN) / PLATEAU_DENSITY_RANGE);
        spawnLandMask = new ConcurrentBinaryMask(spawnMasks[0], random.nextLong(), "spawnsLand");
        spawnPlateauMask = new ConcurrentBinaryMask(spawnMasks[1], random.nextLong(), "spawnsPlateau");

        setupTerrainPipeline();
        setupHeightmapPipeline();
        setupTexturePipeline();
        setupPropPipeline();
        setupWreckPipeline();
        setupResourcePipeline();

        random = null;
        Pipeline.start();

        CompletableFuture<Void> aiMarkerFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(passable, passableLand, passableWater);
            long sTime = System.currentTimeMillis();
            aiMarkerGenerator.generateAIMarkers(passable.getFinalMask(), passableLand.getFinalMask(), passableWater.getFinalMask(), mapSize / 32f, mapSize / 32f + 4f);
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateAIMarkers\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });


        CompletableFuture<Void> textureFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(accentGroundTexture, accentPlateauTexture, slopesTexture, accentSlopesTexture, steepHillsTexture, waterBeachTexture, rockTexture, accentRockTexture);
            long sTime = System.currentTimeMillis();
            map.setTextureMasksLowScaled(accentGroundTexture.getFinalMask(), accentPlateauTexture.getFinalMask(), slopesTexture.getFinalMask(), accentSlopesTexture.getFinalMask());
            map.setTextureMasksHighScaled(steepHillsTexture.getFinalMask(), waterBeachTexture.getFinalMask(), rockTexture.getFinalMask(), accentRockTexture.getFinalMask());
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateTextures\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        CompletableFuture<Void> resourcesFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(resourceMask, plateaus, land, ramps, impassable, unbuildable, allWreckMask, plateauResourceMask, waterResourceMask);
            long sTime = System.currentTimeMillis();
            mexGenerator.generateMexes(resourceMask.getFinalMask(), plateauResourceMask.getFinalMask(), waterResourceMask.getFinalMask());
            hydroGenerator.generateHydros(resourceMask.getFinalMask().deflate(4));
            generateExclusionMasks();
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateResources\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        resourcesFuture.join();

        CompletableFuture<Void> wrecksFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(t1LandWreckMask, t2LandWreckMask, t3LandWreckMask, t2NavyWreckMask, navyFactoryWreckMask);
            long sTime = System.currentTimeMillis();

            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateWrecks\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        CompletableFuture<Void> propsFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(treeMask, cliffRockMask, largeRockFieldMask, fieldStoneMask);
            long sTime = System.currentTimeMillis();
            propGenerator.generateProps(treeMask.getFinalMask().minus(noProps), biome.getPropMaterials().getTreeGroups(), 3f);
            propGenerator.generateProps(cliffRockMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), 1.5f);
            propGenerator.generateProps(largeRockFieldMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), 1.5f);
            propGenerator.generateProps(smallRockFieldMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), 1.5f);
            propGenerator.generateProps(fieldStoneMask.getFinalMask().minus(noProps), biome.getPropMaterials().getBoulders(), 30f);
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateProps\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        CompletableFuture<Void> decalsFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(intDecal, rockDecal);
            long sTime = System.currentTimeMillis();
            decalGenerator.generateDecals(intDecal.getFinalMask().minus(noDecals), DecalGenerator.INT, 36f, 18f);
            decalGenerator.generateDecals(rockDecal.getFinalMask().minus(noDecals), DecalGenerator.ROCKS, 16f, 8f);
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateDecals\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        CompletableFuture<Void> baseFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(baseMask, civReclaimMask, t1LandWreckMask, t2LandWreckMask, t3LandWreckMask, t2NavyWreckMask, navyFactoryWreckMask);
            long sTime = System.currentTimeMillis();
            Army army17 = new Army("ARMY_17", new ArrayList<>());
            Group army17Initial = new Group("INITIAL", new ArrayList<>());
            Group army17Wreckage = new Group("WRECKAGE", new ArrayList<>());
            army17.addGroup(army17Initial);
            army17.addGroup(army17Wreckage);
            Army civilian = new Army("NEUTRAL_CIVILIAN", new ArrayList<>());
            Group civilianInitial = new Group("INITIAL", new ArrayList<>());
            civilian.addGroup(civilianInitial);
            map.addArmy(army17);
            map.addArmy(civilian);
            unitGenerator.generateBases(baseMask.getFinalMask().minus(noBases), UnitGenerator.MEDIUM_ENEMY, army17, army17Initial, 512f);
            unitGenerator.generateBases(civReclaimMask.getFinalMask().minus(noCivs), UnitGenerator.MEDIUM_RECLAIM, civilian, civilianInitial, 256f);
            unitGenerator.generateUnits(t1LandWreckMask.getFinalMask().minus(noWrecks), UnitGenerator.T1_Land, army17, army17Wreckage, 3f);
            unitGenerator.generateUnits(t2LandWreckMask.getFinalMask().minus(noWrecks), UnitGenerator.T2_Land, army17, army17Wreckage, 30f);
            unitGenerator.generateUnits(t3LandWreckMask.getFinalMask().minus(noWrecks), UnitGenerator.T3_Land, army17, army17Wreckage, 128f);
            unitGenerator.generateUnits(t2NavyWreckMask.getFinalMask().minus(noWrecks), UnitGenerator.T2_Navy, army17, army17Wreckage, 128f);
            unitGenerator.generateUnits(navyFactoryWreckMask.getFinalMask().minus(noWrecks), UnitGenerator.Navy_Factory, army17, army17Wreckage, 256f);
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateBases\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        CompletableFuture<Void> heightMapFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(heightmapBase);
            long sTime = System.currentTimeMillis();
            map.setHeightImage(heightmapBase.getFinalMask());
            map.getHeightmap().getRaster().setPixel(0, 0, new int[]{0});
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, setHeightmap\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        wrecksFuture.join();
        propsFuture.join();
        decalsFuture.join();
        aiMarkerFuture.join();
        heightMapFuture.join();
        baseFuture.join();

        CompletableFuture<Void> placementFuture = CompletableFuture.runAsync(() -> {
            long sTime = System.currentTimeMillis();
            spawnGenerator.setMarkerHeights();
            mexGenerator.setMarkerHeights();
            hydroGenerator.setMarkerHeights();
            propGenerator.setPropHeights();
            decalGenerator.setDecalHeights();
            unitGenerator.setUnitHeights();
            aiMarkerGenerator.setMarkerHeights();
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, setPlacements\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        textureFuture.join();
        placementFuture.join();
        Pipeline.stop();
        long sTime = System.currentTimeMillis();
        map.setGeneratePreview(!blind);
        if (!blind) {
            PreviewGenerator.generate(map.getPreview(), map);
        } else {
            BufferedImage blindPreview = readImage(BLANK_PREVIEW);
            map.getPreview().setData(blindPreview.getData());
        }
        if (tournamentStyle) {
            map.setDescription(String.format("Map originally generated at %s", Instant.ofEpochSecond(generationTime).toString()));
        }
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, generatePreview\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(MapGenerator.class));
        }

        System.out.printf("Map generation done: %d ms\n", System.currentTimeMillis() - startTime);

        return map;
    }

    private void setupTerrainPipeline() {
        land = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetrySettings, "land");
        mountains = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "mountains");
        plateaus = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetrySettings, "plateaus");
        ramps = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "ramps");
        ConcurrentBinaryMask spawnRamps = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "spawnRamps");

        land.randomize(landDensity).smooth(mapSize / 256, .75f);

        if (random.nextBoolean()) {
            mountains.progressiveWalk((int) (mountainDensity * mapSize / 8), mapSize / 8);
        } else {
            mountains.randomWalk((int) (mountainDensity * mapSize / 8), mapSize / 8);
        }
        mountains.setSize(mapSize / 4).erode(.5f, symmetrySettings.getTerrainSymmetry(), 2).grow(.5f, symmetrySettings.getTerrainSymmetry(), 4);
        plateaus.randomize(plateauDensity).smooth(mapSize / 128);

        land.setSize(mapSize / 4).erode(.5f, symmetrySettings.getTerrainSymmetry(), mapSize / 256).grow(.5f, symmetrySettings.getTerrainSymmetry(), mapSize / 256);
        plateaus.setSize(mapSize / 4).intersect(land).erode(.5f, symmetrySettings.getTerrainSymmetry(), mapSize / 256).grow(.5f, symmetrySettings.getTerrainSymmetry(), mapSize / 64);

        land.setSize(mapSize + 1).smooth(8, .9f);
        mountains.setSize(mapSize + 1);
        plateaus.setSize(mapSize + 1).intersect(land).smooth(12, .1f);

        spawnPlateauMask.shrink(mapSize / 4).erode(.5f, symmetrySettings.getSpawnSymmetry(), 4).grow(.5f, symmetrySettings.getSpawnSymmetry(), 6);
        spawnPlateauMask.erode(.5f, symmetrySettings.getSpawnSymmetry()).setSize(mapSize + 1).smooth(4);

        if (mapSize <= 512) {
            spawnLandMask.shrink(mapSize / 4).erode(.25f, symmetrySettings.getSpawnSymmetry(), 4).grow(.5f, symmetrySettings.getSpawnSymmetry(), 6);
            spawnLandMask.erode(.5f, symmetrySettings.getSpawnSymmetry()).setSize(mapSize + 1).smooth(4);
        } else {
            spawnLandMask.shrink(mapSize / 16).erode(.5f, symmetrySettings.getSpawnSymmetry(), 2).grow(.5f, symmetrySettings.getSpawnSymmetry(), 6);
            spawnLandMask.setSize(mapSize / 4).erode(.5f, symmetrySettings.getSpawnSymmetry()).setSize(mapSize + 1).smooth(8);
            spawnPlateauMask.clear();
        }


        plateaus.minus(spawnLandMask).combine(spawnPlateauMask).removeAreasSmallerThan(512);
        land.combine(spawnLandMask).combine(spawnPlateauMask);

        boolean fillLandGaps = (random.nextFloat() < (landDensity - LAND_DENSITY_MIN) / LAND_DENSITY_RANGE) || mapSize > 512;
        int fillSize = map.getSize() / 8;

        if (fillLandGaps) {
            land.fillGaps(fillSize);
        } else {
            land.widenGaps(fillSize);
        }

        if (random.nextBoolean()) {
            plateaus.fillGaps(fillSize);
        } else {
            plateaus.widenGaps(fillSize);
        }

        plateaus.minus(spawnLandMask).combine(spawnPlateauMask);
        land.combine(spawnLandMask).combine(spawnPlateauMask);
        if (fillLandGaps) {
            land.widenGaps(fillSize);
        }

        land.removeAreasSmallerThan(mapSize * mapSize / 256);

        mountains.minus(spawnLandMask);

        ramps.randomize(rampDensity).setSize(mapSize + 1);
        ramps.intersect(plateaus.copy().outline()).minus(mountains.copy().inflate(8)).inflate(16);

        spawnRamps.combine(spawnLandMask.copy().outline()).combine(spawnPlateauMask.copy().outline()).inflate(32).intersect(plateaus.copy().outline()).flipValues(.01f).inflate(16);

        ramps.combine(spawnRamps).smooth(8, .125f).fillGaps(32);

        mountains.minus(plateaus.copy().outline().inflate(64)).minus(land.copy().outline().inflate(64)).smooth(8).intersect(land).removeAreasSmallerThan(256);
        if (mountainDensity < .25) {
            mountains.fillGaps(24);
        } else if (mountainDensity < .5) {
            mountains.widenGaps(24);
        } else if (mountainDensity < .75) {
            mountains.acid(.00005f, 24).widenGaps(24);
        } else {
            mountains.acid(.0001f, 24).widenGaps(24);
        }
        mountains.removeAreasSmallerThan(128);
        plateaus.intersect(land).fillGaps(fillSize / 2).minus(spawnLandMask).combine(spawnPlateauMask).combine(mountains).removeAreasSmallerThan(mapSize * mapSize / 256);
        land.combine(plateaus).combine(spawnLandMask).combine(spawnPlateauMask);

        ConcurrentBinaryMask plateauOutline = plateaus.copy().outline().minus(ramps).minus(mountains.copy().inflate(1));
        ConcurrentBinaryMask landOutline = land.copy().outline().minus(plateaus.copy().inflate(1));

        ConcurrentBinaryMask shoreLine = landOutline.copy();

        shoreLine.setSize(mapSize / 4).erode(.75f, symmetrySettings.getSpawnSymmetry()).grow(.5f, symmetrySettings.getSpawnSymmetry(), 4).setSize(mapSize + 1).erode(.25f, symmetrySettings.getSpawnSymmetry(), 2);
        shoreLine.combine(landOutline.copy().flipValues(random.nextFloat() * .01f).grow(.5f, symmetrySettings.getSpawnSymmetry(), 18)).minus(ramps).smooth(2, .75f);

        land.combine(shoreLine);
        landOutline = land.copy().outline().minus(plateaus.copy().inflate(1));

        cliffs = plateauOutline.copy();
        shore = landOutline.copy();

        cliffs.setSize(mapSize / 4).erode(.75f, symmetrySettings.getSpawnSymmetry()).grow(.5f, symmetrySettings.getSpawnSymmetry(), 4).setSize(mapSize + 1).erode(.25f, symmetrySettings.getSpawnSymmetry(), 2);
        cliffs.combine(plateauOutline.copy().flipValues(random.nextFloat() * .01f).grow(.5f, symmetrySettings.getSpawnSymmetry(), 18)).minus(ramps).smooth(2, .75f);

        shore.setSize(mapSize / 4).flipValues(random.nextFloat() * .15f).grow(.5f, symmetrySettings.getSpawnSymmetry(), 8).setSize(mapSize + 1).intersect(landOutline.copy().inflate(6));
        shore.minus(ramps).smooth(2, .75f);

        plateaus.combine(cliffs);

        hills = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "hills");
        valleys = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "valleys");

        hills.randomWalk(random.nextInt(5) + 3, random.nextInt(500) + 350).setSize(mapSize + 1).smooth(10, .25f).intersect(land.copy().deflate(8)).minus(plateaus);
        valleys.randomWalk(random.nextInt(5) + 3, random.nextInt(500) + 350).setSize(mapSize + 1).smooth(10, .25f).intersect(plateaus.copy().deflate(4));
    }

    private void setupHeightmapPipeline() {
        heightmapBase = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapBase");
        ConcurrentFloatMask heightmapLand = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapLand");
        ConcurrentFloatMask heightmapMountains = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapMountains");
        ConcurrentFloatMask heightmapPlateaus = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapPlateaus");
        ConcurrentFloatMask heightmapCliffs = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapCliffs");
        ConcurrentFloatMask heightmapShore = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapShore");
        ConcurrentFloatMask heightmapHills = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapHills");
        ConcurrentFloatMask heightmapValleys = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapValleys");
        ConcurrentBinaryMask oceanFloor = land.copy().invert();

        oceanFloor.acid(.01f, 1).erode(.75f, symmetrySettings.getSpawnSymmetry(), 2).smooth(32, .75f).invert().removeAreasSmallerThan(128);

        heightmapBase.init(land, waterHeight + .5f, waterHeight + .5f);
        heightmapPlateaus.init(plateaus, 0, PLATEAU_HEIGHT).smooth(8, ramps).smooth(1);
        heightmapHills.maskToHills(hills).clampMax(HILL_HEIGHT).smooth(16, land.copy().minus(plateaus));
        heightmapValleys.maskToHills(valleys).multiply(-1).clampMin(VALLEY_HEIGHT).smooth(16, plateaus);
        heightmapLand.add(heightmapHills).add(heightmapValleys).smooth(2).maskToOceanHeights(0.35f, land).clampMin(biome.getWaterSettings().getElevationAbyss() - waterHeight + 1f).maskToOceanHeights(0.15f, oceanFloor).clampMin(biome.getWaterSettings().getElevationAbyss() - waterHeight - 1f);
        heightmapCliffs.init(cliffs, 0, 1f).maskToMountains(cliffs);
        heightmapShore.init(shore, 0, 1.5f).maskToMountains(shore);
        heightmapMountains.maskToMountains(mountains).smooth(2);

        ConcurrentBinaryMask mountainsPresent = new ConcurrentBinaryMask(heightmapMountains, 2f, null, "mountainsPresent");

        heightmapMountains.add(mountainsPresent, 3f);
        heightmapMountains.add(heightmapLand).add(heightmapCliffs).add(heightmapShore).smooth(2).add(heightmapPlateaus).smooth(1);

        heightmapBase.add(heightmapMountains);
        slope = heightmapBase.copy().gradient();

        impassable = new ConcurrentBinaryMask(slope, 1f, random.nextLong(), "impassable");
        unbuildable = new ConcurrentBinaryMask(slope, .5f, random.nextLong(), "unbuildable");

        impassable.inflate(2);

        passable = new ConcurrentBinaryMask(impassable, random.nextLong(), "passable").invert();
        passableLand = new ConcurrentBinaryMask(land, random.nextLong(), "passableLand");
        passableWater = new ConcurrentBinaryMask(land, random.nextLong(), "passableWater").invert();

        passable.deflate(mapSize / 64f).trimEdge(8);
        passableLand.deflate(4).intersect(passable);
        passableWater.deflate(16).trimEdge(8);
    }

    private void setupResourcePipeline() {
        resourceMask = new ConcurrentBinaryMask(land, random.nextLong(), "resource");
        waterResourceMask = new ConcurrentBinaryMask(land, random.nextLong(), "waterResource").invert();
        plateauResourceMask = new ConcurrentBinaryMask(land, random.nextLong(), "plateauResource");

        resourceMask.minus(unbuildable).deflate(8);
        resourceMask.trimEdge(16).fillCenter(24, false);
        waterResourceMask.minus(unbuildable).deflate(8).trimEdge(16).fillCenter(24, false);
        plateauResourceMask.combine(resourceMask).intersect(plateaus).trimEdge(16).fillCenter(24, false);
    }

    private void setupTexturePipeline() {
        ConcurrentBinaryMask flat = new ConcurrentBinaryMask(slope, .05f, random.nextLong(), "flat").invert();
        ConcurrentBinaryMask inland = new ConcurrentBinaryMask(land, random.nextLong(), "inland");
        ConcurrentBinaryMask highGround = new ConcurrentBinaryMask(heightmapBase, waterHeight + 3f, random.nextLong(), "highGround");
        ConcurrentBinaryMask aboveBeach = new ConcurrentBinaryMask(heightmapBase, waterHeight + 1.5f, random.nextLong(), "aboveBeach");
        ConcurrentBinaryMask aboveBeachEdge = new ConcurrentBinaryMask(heightmapBase, waterHeight + 3f, random.nextLong(), "aboveBeachEdge");
        ConcurrentBinaryMask flatAboveCoast = new ConcurrentBinaryMask(heightmapBase, waterHeight + 0.29f, random.nextLong(), "flatAboveCoast");
        ConcurrentBinaryMask higherFlatAboveCoast = new ConcurrentBinaryMask(heightmapBase, waterHeight + 1.2f, random.nextLong(), "higherFlatAboveCoast");
        ConcurrentBinaryMask lowWaterBeach = new ConcurrentBinaryMask(heightmapBase, waterHeight, random.nextLong(), "lowWaterBeach");
        ConcurrentBinaryMask waterBeach = new ConcurrentBinaryMask(heightmapBase, waterHeight + 1f, random.nextLong(), "waterBeach");
        ConcurrentBinaryMask accentGround = new ConcurrentBinaryMask(land, random.nextLong(), "accentGround");
        ConcurrentBinaryMask accentPlateau = new ConcurrentBinaryMask(plateaus, random.nextLong(), "accentPlateau");
        ConcurrentBinaryMask slopes = new ConcurrentBinaryMask(slope, .1f, random.nextLong(), "slopes");
        ConcurrentBinaryMask accentSlopes = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "accentSlopes").invert();
        ConcurrentBinaryMask steepHills = new ConcurrentBinaryMask(slope, .55f, random.nextLong(), "steepHills");
        ConcurrentBinaryMask rock = new ConcurrentBinaryMask(slope, 1.25f, random.nextLong(), "rock");
        ConcurrentBinaryMask accentRock = new ConcurrentBinaryMask(slope, 1.25f, random.nextLong(), "accentRock");
        intDecal = new ConcurrentBinaryMask(land, random.nextLong(), "intDecal");
        rockDecal = new ConcurrentBinaryMask(mountains, random.nextLong(), "rockDecal");
        waterBeachTexture = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "waterBeachTexture");
        accentGroundTexture = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "accentGroundTexture");
        accentPlateauTexture = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "accentPlateauTexture");
        slopesTexture = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "slopesTexture");
        accentSlopesTexture = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "accentSlopesTexture");
        steepHillsTexture = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "steepHillsTexture");
        rockTexture = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "rockTexture");
        accentRockTexture = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "accentRockTexture");

        inland.deflate(2);
        flatAboveCoast.intersect(flat);
        higherFlatAboveCoast.intersect(flat);
        lowWaterBeach.invert().inflate(6).minus(aboveBeach);
        waterBeach.invert().minus(flatAboveCoast).minus(inland).inflate(1).combine(lowWaterBeach).smooth(5, 0.5f).minus(aboveBeach).minus(higherFlatAboveCoast).smooth(2).smooth(1);
        accentGround.minus(highGround).acid(.1f, 0).erode(.4f, symmetrySettings.getSpawnSymmetry()).smooth(3, .75f);
        accentPlateau.acid(.05f, 0).erode(.85f, symmetrySettings.getSpawnSymmetry()).smooth(2, .75f).acid(.45f, 0);
        slopes.intersect(land).flipValues(.95f).erode(.5f, symmetrySettings.getSpawnSymmetry()).acid(.3f, 0).erode(.2f, symmetrySettings.getSpawnSymmetry());
        accentSlopes.minus(flat).intersect(land).acid(.1f, 0).erode(.5f, symmetrySettings.getSpawnSymmetry()).smooth(4, .75f).acid(.55f, 0);
        steepHills.acid(.3f, 0).erode(.2f, symmetrySettings.getSpawnSymmetry());
        accentRock.acid(.2f, 0).erode(.3f, symmetrySettings.getSpawnSymmetry()).acid(.2f, 0).smooth(2, .5f).intersect(rock);

        waterBeachTexture.init(waterBeach, 0, 1).subtract(rock, 1f).subtract(aboveBeachEdge, 1f).clampMin(0).smooth(2, rock.copy().invert()).add(waterBeach, 1f).subtract(rock, 1f);
        waterBeachTexture.subtract(aboveBeachEdge, .9f).clampMin(0).smooth(2, rock.copy().invert()).subtract(rock, 1f).subtract(aboveBeachEdge, .8f).clampMin(0).add(waterBeach, .65f).smooth(2, rock.copy().invert());
        waterBeachTexture.subtract(rock, 1f).subtract(aboveBeachEdge, 0.7f).clampMin(0).add(waterBeach, .5f).smooth(2, rock.copy().invert()).smooth(2, rock.copy().invert()).subtract(rock, 1f).clampMin(0).smooth(2, rock.copy().invert());
        waterBeachTexture.smooth(2, rock.copy().invert()).subtract(rock, 1f).clampMin(0).smooth(2, rock.copy().invert()).smooth(1, rock.copy().invert()).smooth(1, rock.copy().invert()).clampMax(1f).threshold(.1f).smooth(2);
        accentGroundTexture.init(accentGround, 0, 1).smooth(16).add(accentGround, .65f).smooth(8).add(accentGround, .5f).smooth(2).clampMax(1f).threshold(.1f).smooth(2);
        accentPlateauTexture.init(accentPlateau, 0, 1).smooth(16).add(accentPlateau, .65f).smooth(8).add(accentPlateau, .5f).smooth(2).clampMax(1f).threshold(.1f).smooth(2);
        slopesTexture.init(slopes, 0, 1).smooth(32).add(slopes, .65f).smooth(16).add(slopes, .5f).smooth(4).clampMax(1f).threshold(.05f).smooth(2);
        accentSlopesTexture.init(accentSlopes, 0, 1).smooth(16).add(accentSlopes, .65f).smooth(8).add(accentSlopes, .5f).smooth(2).clampMax(1f).threshold(.05f).smooth(2);
        steepHillsTexture.init(steepHills, 0, 1).smooth(8).clampMax(0.35f).add(steepHills, .65f).smooth(4).clampMax(0.65f).add(steepHills, .5f).smooth(1).clampMax(1f).threshold(.1f).smooth(2);
        rockTexture.init(rock, 0, 1).smooth(8).clampMax(0.2f).add(rock, .65f).smooth(4).clampMax(0.3f).add(rock, .5f).smooth(1).add(rock, 1f).clampMax(1f).threshold(.1f).smooth(2);
        accentRockTexture.init(accentRock, 0, 1).subtract(waterBeachTexture).clampMin(0).smooth(8).add(accentRock, .65f).smooth(4).add(accentRock, .5f).smooth(1).clampMax(1f).threshold(.1f).smooth(2);

    }

    private void setupPropPipeline() {
        baseMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "base");
        civReclaimMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "civReclaim");
        allBaseMask = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "allBase");
        treeMask = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetrySettings, "tree");
        cliffRockMask = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetrySettings, "cliffRock");
        fieldStoneMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "fieldStone");
        largeRockFieldMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "largeRockField");
        smallRockFieldMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "smallRockField");

        if (hasCivilians) {
            if (!enemyCivilians) {
                baseMask.setSize(mapSize + 1);
                civReclaimMask.randomize(.005f).setSize(mapSize + 1).intersect(land.copy().minus(unbuildable).minus(ramps).deflate(24)).fillCenter(32, false).trimEdge(64);
            } else {
                civReclaimMask.setSize(mapSize + 1);
                baseMask.randomize(.005f).setSize(mapSize + 1).intersect(land.copy().minus(unbuildable).minus(ramps).deflate(24)).fillCenter(32, false).trimEdge(32).minus(civReclaimMask.copy().inflate(16));
            }
        } else {
            civReclaimMask.setSize(mapSize + 1);
            baseMask.setSize(mapSize + 1);
        }
        allBaseMask.combine(baseMask.copy().inflate(24)).combine(civReclaimMask.copy().inflate(24));

        cliffRockMask.randomize(.4f).setSize(mapSize + 1).intersect(impassable).grow(.5f, symmetrySettings.getSpawnSymmetry(), 4).minus(plateaus.copy().outline()).intersect(land);
        fieldStoneMask.randomize(reclaimDensity * .001f).setSize(mapSize + 1).intersect(land).minus(impassable).trimEdge(10);
        treeMask.randomize(.2f).setSize(mapSize / 4).inflate(2).erode(.5f, symmetrySettings.getSpawnSymmetry()).smooth(4, .75f).erode(.5f, symmetrySettings.getSpawnSymmetry());
        treeMask.setSize(mapSize + 1).intersect(land.copy().deflate(8)).minus(impassable.copy().inflate(2)).deflate(2).trimEdge(8).smooth(4, .25f);
        largeRockFieldMask.randomize(reclaimDensity * .001f).trimEdge(mapSize / 16).grow(.5f, symmetrySettings.getSpawnSymmetry(), 3).setSize(mapSize + 1).intersect(land).minus(impassable);
        smallRockFieldMask.randomize(reclaimDensity * .003f).trimEdge(mapSize / 64).grow(.5f, symmetrySettings.getSpawnSymmetry()).setSize(mapSize + 1).intersect(land).minus(impassable);
    }

    private void setupWreckPipeline() {
        t1LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t1LandWreck");
        t2LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t2LandWreck");
        t3LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t3LandWreck");
        t2NavyWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t2NavyWreck");
        navyFactoryWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "navyFactoryWreck");
        allWreckMask = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "allWreck");

        t1LandWreckMask.randomize(reclaimDensity * .005f).setSize(mapSize + 1).intersect(land).deflate(mapSize / 512f).trimEdge(20);
        t2LandWreckMask.randomize(reclaimDensity * .0025f).setSize(mapSize + 1).intersect(land).minus(t1LandWreckMask).trimEdge(64);
        t3LandWreckMask.randomize(reclaimDensity * .00025f).setSize(mapSize + 1).intersect(land).minus(t1LandWreckMask).minus(t2LandWreckMask).trimEdge(mapSize / 8);
        navyFactoryWreckMask.randomize(reclaimDensity * .005f).setSize(mapSize + 1).minus(land.copy().inflate(16)).trimEdge(20);
        t2NavyWreckMask.randomize(reclaimDensity * .005f).setSize(mapSize + 1).intersect(land.copy().inflate(4).outline()).trimEdge(20);
        allWreckMask.combine(t1LandWreckMask).combine(t2LandWreckMask).combine(t3LandWreckMask).combine(t2NavyWreckMask).inflate(2);
    }

    private void generateExclusionMasks() {
        noProps = new BinaryMask(impassable.getFinalMask(), null);
        noProps.combine(ramps.getFinalMask());

        for (int i = 0; i < map.getSpawnCount(); i++) {
            noProps.fillCircle(map.getSpawn(i).getPosition(), 30, true);
        }
        for (int i = 0; i < map.getMexCount(); i++) {
            noProps.fillCircle(map.getMex(i).getPosition(), 1, true);
        }
        for (int i = 0; i < map.getHydroCount(); i++) {
            noProps.fillCircle(map.getHydro(i).getPosition(), 8, true);
        }

        noProps.combine(allWreckMask.getFinalMask()).combine(allBaseMask.getFinalMask());

        noBases = new BinaryMask(unbuildable.getFinalMask(), null);
        noBases.combine(ramps.getFinalMask());

        for (int i = 0; i < map.getSpawnCount(); i++) {
            noBases.fillCircle(map.getSpawn(i).getPosition(), 128, true);
        }
        for (int i = 0; i < map.getMexCount(); i++) {
            noBases.fillCircle(map.getMex(i).getPosition(), 32, true);
        }
        for (int i = 0; i < map.getHydroCount(); i++) {
            noBases.fillCircle(map.getHydro(i).getPosition(), 32, true);
        }

        noCivs = new BinaryMask(unbuildable.getFinalMask(), null);
        noCivs.combine(ramps.getFinalMask());

        for (int i = 0; i < map.getSpawnCount(); i++) {
            noCivs.fillCircle(map.getSpawn(i).getPosition(), 96, true);
        }
        for (int i = 0; i < map.getMexCount(); i++) {
            noCivs.fillCircle(map.getMex(i).getPosition(), 32, true);
        }
        for (int i = 0; i < map.getHydroCount(); i++) {
            noCivs.fillCircle(map.getHydro(i).getPosition(), 32, true);
        }

        noWrecks = new BinaryMask(impassable.getFinalMask(), null);

        noWrecks.combine(allBaseMask.getFinalMask());

        for (int i = 0; i < map.getSpawnCount(); i++) {
            noWrecks.fillCircle(map.getSpawn(i).getPosition(), 128, true);
        }
        for (int i = 0; i < map.getMexCount(); i++) {
            noWrecks.fillCircle(map.getMex(i).getPosition(), 8, true);
        }
        for (int i = 0; i < map.getHydroCount(); i++) {
            noWrecks.fillCircle(map.getHydro(i).getPosition(), 32, true);
        }

        noDecals = new BinaryMask(mapSize + 1, null, symmetrySettings);

        for (int i = 0; i < map.getSpawnCount(); i++) {
            noDecals.fillCircle(map.getSpawn(i).getPosition(), 24, true);
        }
    }

    public void toFile(Path path) throws IOException {

        Files.deleteIfExists(path);
        File outFile = path.toFile();
        boolean status = outFile.createNewFile();
        if (status) {
            FileOutputStream out = new FileOutputStream(outFile);
            String summaryString = "Seed: " + seed +
                    "\nBiome: " + biome.getName() +
                    "\nLand Density: " + landDensity +
                    "\nPlateau Density: " + plateauDensity +
                    "\nMountain Density: " + mountainDensity +
                    "\nRamp Density: " + rampDensity +
                    "\nReclaim Density: " + reclaimDensity +
                    "\nMex Count: " + mexCount +
                    "\nTerrain Symmetry: " + terrainSymmetry +
                    "\nTeam Symmetry: " + symmetrySettings.getTeamSymmetry() +
                    "\nSpawn Symmetry: " + symmetrySettings.getSpawnSymmetry();
            out.write(summaryString.getBytes());
            out.flush();
            out.close();
        }
    }
}
