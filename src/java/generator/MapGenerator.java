package generator;

import biomes.Biome;
import biomes.Biomes;
import brushes.Brushes;
import com.google.common.io.BaseEncoding;
import exporter.MapExporter;
import exporter.SCMapExporter;
import lombok.Getter;
import lombok.Setter;
import map.*;
import util.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static util.ImageUtils.readImage;

@Getter
@Setter
public strictfp class MapGenerator {

    public static final String VERSION = "1.1.18";
    private static final String BLANK_PREVIEW = "/images/generatedMapIcon.png";
    public static final BaseEncoding NAME_ENCODER = BaseEncoding.base32().omitPadding().lowerCase();
    public static final float LAND_DENSITY_MIN = .8f;
    public static final float LAND_DENSITY_MAX = .95f;
    public static final float LAND_DENSITY_RANGE = LAND_DENSITY_MAX - LAND_DENSITY_MIN;
    public static final float MOUNTAIN_DENSITY_MIN = 0f;
    public static final float MOUNTAIN_DENSITY_MAX = 1f;
    public static final float MOUNTAIN_DENSITY_RANGE = MOUNTAIN_DENSITY_MAX - MOUNTAIN_DENSITY_MIN;
    public static final float RAMP_DENSITY_MIN = 0f;
    public static final float RAMP_DENSITY_MAX = 1f;
    public static final float RAMP_DENSITY_RANGE = RAMP_DENSITY_MAX - RAMP_DENSITY_MIN;
    public static final float PLATEAU_DENSITY_MIN = .35f;
    public static final float PLATEAU_DENSITY_MAX = .5f;
    public static final float PLATEAU_DENSITY_RANGE = PLATEAU_DENSITY_MAX - PLATEAU_DENSITY_MIN;
    public static final float PLATEAU_HEIGHT = 7f;
    public static final float OCEAN_FLOOR = -15f;
    public static final float VALLEY_FLOOR = -5f;
    public static final float LAND_HEIGHT = .5f;
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

        System.out.println(generator.mapName);
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
        setupSymmetrySettings();
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
                    "--symmetry arg         optional, set the symmetry for the generated map (POINT2, POINT4, QUAD, DIAG, X, Z, XZ, ZX)\n" +
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

        if (arguments.containsKey("map-name") && arguments.get("map-name") != null) {
            mapName = arguments.get("map-name");
            parseMapName();
            return;
        }

        if (arguments.containsKey("seed") && arguments.get("seed") != null) {
            seed = Long.parseLong(arguments.get("seed"));
            random = new Random(seed);
        }

        if (arguments.containsKey("spawn-count") && arguments.get("spawn-count") != null) {
            spawnCount = Integer.parseInt(arguments.get("spawn-count"));
        }

        if (arguments.containsKey("map-size") && arguments.get("map-size") != null) {
            mapSize = Integer.parseInt(arguments.get("map-size"));
        }

        randomizeOptions();

        tournamentStyle = arguments.containsKey("tournament-style") || arguments.containsKey("blind");
        blind = arguments.containsKey("blind");

        if (arguments.containsKey("land-density") && arguments.get("land-density") != null) {
            landDensity = Float.parseFloat(arguments.get("land-density"));
            landDensity = (float) StrictMath.round(landDensity * 127f) / 127f * LAND_DENSITY_RANGE + LAND_DENSITY_MIN;
        }

        if (arguments.containsKey("plateau-density") && arguments.get("plateau-density") != null) {
            plateauDensity = Float.parseFloat(arguments.get("plateau-density"));
            plateauDensity = (float) StrictMath.round(plateauDensity * 127f) / 127f * PLATEAU_DENSITY_RANGE + PLATEAU_DENSITY_MIN;
        }

        if (arguments.containsKey("mountain-density") && arguments.get("mountain-density") != null) {
            mountainDensity = Float.parseFloat(arguments.get("mountain-density"));
            mountainDensity = (float) StrictMath.round(mountainDensity * 127f) / 127f * MOUNTAIN_DENSITY_RANGE + MOUNTAIN_DENSITY_MIN;
        }

        if (arguments.containsKey("ramp-density") && arguments.get("ramp-density") != null) {
            rampDensity = Float.parseFloat(arguments.get("ramp-density"));
            rampDensity = (float) StrictMath.round(rampDensity * 127f) / 127f * RAMP_DENSITY_RANGE + RAMP_DENSITY_MIN;
        }

        if (arguments.containsKey("reclaim-density") && arguments.get("reclaim-density") != null) {
            reclaimDensity = Float.parseFloat(arguments.get("reclaim-density"));
            reclaimDensity = (float) StrictMath.round(reclaimDensity * 127f) / 127f;
        }

        if (arguments.containsKey("mex-count") && arguments.get("mex-count") != null) {
            mexCount = Integer.parseInt(arguments.get("mex-count"));
        }

        if (arguments.containsKey("symmetry") && arguments.get("symmetry") != null) {
            terrainSymmetry = Symmetry.valueOf(arguments.get("symmetry"));
        }

        if (arguments.containsKey("map-size") && arguments.get("map-size") != null) {
            mapSize = Integer.parseInt(arguments.get("map-size"));
        }

        if (arguments.containsKey("biome") && arguments.get("biome") != null) {
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
                case 2 -> 2f;
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
        List<Symmetry> terrainSymmetries;
        if (spawnCount == 2) {
            terrainSymmetries = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.POINT4, Symmetry.QUAD, Symmetry.DIAG));
        } else {
            terrainSymmetries = new ArrayList<>(Arrays.asList(Symmetry.values()));
        }
        terrainSymmetries.remove(Symmetry.NONE);
        terrainSymmetry = terrainSymmetries.get(random.nextInt(terrainSymmetries.size()));
        biome = Biomes.getRandomBiome(random);
    }

    private void setupSymmetrySettings() {
        Symmetry spawnSymmetry;
        Symmetry teamSymmetry;
        List<Symmetry> spawns;
        List<Symmetry> teams;
        switch (terrainSymmetry) {
            case POINT2 -> {
                spawnSymmetry = terrainSymmetry;
                teams = new ArrayList<>(Arrays.asList(Symmetry.X, Symmetry.Z, Symmetry.XZ, Symmetry.ZX));
                teamSymmetry = teams.get(random.nextInt(teams.size()));
            }
            case POINT4 -> {
                spawns = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.POINT4));
                if (spawnCount % 4 != 0) {
                    spawns.remove(Symmetry.POINT4);
                }
                spawnSymmetry = spawns.get(random.nextInt(spawns.size()));
                teams = new ArrayList<>(Arrays.asList(Symmetry.X, Symmetry.Z, Symmetry.XZ, Symmetry.ZX));
                teamSymmetry = teams.get(random.nextInt(teams.size()));
            }
            case QUAD -> {
                spawns = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.QUAD));
                if (spawnCount % 4 != 0) {
                    spawns.remove(Symmetry.QUAD);
                }
                spawnSymmetry = spawns.get(random.nextInt(spawns.size()));
                teams = new ArrayList<>(Arrays.asList(Symmetry.X, Symmetry.Z));
                teamSymmetry = teams.get(random.nextInt(teams.size()));
            }
            case DIAG -> {
                spawns = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.DIAG));
                if (spawnCount % 4 != 0) {
                    spawns.remove(Symmetry.DIAG);
                }
                spawnSymmetry = spawns.get(random.nextInt(spawns.size()));
                teams = new ArrayList<>(Arrays.asList(Symmetry.XZ, Symmetry.ZX));
                teamSymmetry = teams.get(random.nextInt(teams.size()));
            }
            default -> {
                spawnSymmetry = terrainSymmetry;
                teamSymmetry = terrainSymmetry;
            }
        }
        symmetrySettings = new SymmetrySettings(terrainSymmetry, teamSymmetry, spawnSymmetry);
        if (spawnCount == 2) {
            symmetrySettings.setSpawnSymmetry(Symmetry.POINT2);
        }
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
                (byte) ((rampDensity - RAMP_DENSITY_MIN) / RAMP_DENSITY_RANGE * 127f)};
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
            MapExporter.exportMap(folderPath.resolve(mapName), mapName, map, tournamentStyle);
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

        final int spawnSize = 32;
        final int hydroCount = spawnCount >= 4 ? spawnCount + random.nextInt(spawnCount / 4) * 2 : spawnCount;
        int mexSpacing = mapSize / 12;
        if (mapSize > 512) {
            landDensity = landDensity - .05f;
        }
        mexSpacing *= StrictMath.min(StrictMath.max(36f / (mexCount * spawnCount), .5f), 1.75f);
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
            case Z, X -> StrictMath.max(StrictMath.max(random.nextInt(map.getSize() / 4 - map.getSize() / 32) + map.getSize() / 32, map.getSize() / spawnCount), 48);
            case NONE -> mapSize / spawnCount * 2;
            default -> StrictMath.max(random.nextInt(map.getSize() / 4 - map.getSize() / 32) + map.getSize() / 32, 48);
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
            CompletableFuture<Void> AmphibiousMarkers = CompletableFuture.runAsync(() -> aiMarkerGenerator.generateAIMarkers(passable.getFinalMask(), map.getAmphibiousAIMarkers(), "AmphPN%d"));
            CompletableFuture<Void> LandMarkers = CompletableFuture.runAsync(() -> aiMarkerGenerator.generateAIMarkers(passableLand.getFinalMask(), map.getLandAIMarkers(), "LandPN%d"));
            CompletableFuture<Void> NavyMarkers = CompletableFuture.runAsync(() -> aiMarkerGenerator.generateAIMarkers(passableWater.getFinalMask(), map.getNavyAIMarkers(), "NavyPN%d"));
            CompletableFuture<Void> AirMarkers = CompletableFuture.runAsync(aiMarkerGenerator::generateAirAIMarkers);
            AmphibiousMarkers.join();
            LandMarkers.join();
            NavyMarkers.join();
            AirMarkers.join();
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
            mexGenerator.generateMexes(resourceMask.getFinalMask(), waterResourceMask.getFinalMask());
            hydroGenerator.generateHydros(resourceMask.getFinalMask().deflate(4));
            generateExclusionMasks();
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateResources\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        resourcesFuture.join();

        CompletableFuture<Void> propsFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(treeMask, cliffRockMask, largeRockFieldMask, fieldStoneMask);
            long sTime = System.currentTimeMillis();
            propGenerator.generateProps(treeMask.getFinalMask().minus(noProps), biome.getPropMaterials().getTreeGroups(), 5f);
            propGenerator.generateProps(cliffRockMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), 2.5f);
            propGenerator.generateProps(largeRockFieldMask.getFinalMask().minus(noProps.copy().inflate(16)), biome.getPropMaterials().getRocks(), 1.5f);
            propGenerator.generateProps(smallRockFieldMask.getFinalMask().minus(noProps.copy().inflate(16)), biome.getPropMaterials().getRocks(), 1.5f);
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
            decalGenerator.generateDecals(intDecal.getFinalMask().minus(noDecals), DecalGenerator.INT, 64f, 18f);
            decalGenerator.generateDecals(rockDecal.getFinalMask().minus(noDecals), DecalGenerator.ROCKS, 32f, 8f);
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateDecals\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        CompletableFuture<Void> unitsFuture = CompletableFuture.runAsync(() -> {
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

        propsFuture.join();
        decalsFuture.join();
        aiMarkerFuture.join();
        heightMapFuture.join();
        unitsFuture.join();

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
            map.setDescription(String.format("Map originally generated at %s UTC",
                    DateTimeFormatter.ofPattern("HH:mm:ss dd MMM uuuu")
                            .format(Instant.ofEpochSecond(generationTime).atZone(ZoneOffset.UTC))));
        }
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, generatePreview\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(MapGenerator.class));
        }

        System.out.printf("Map generation done: %d ms\n", System.currentTimeMillis() - startTime);

        map.addBlank(new BlankMarker(mapName, new Vector2f(0, 0)));
        map.addDecalGroup(new DecalGroup(mapName, new int[0]));

        return map;
    }

    private void setupTerrainPipeline() {
        land = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetrySettings, "land");
        mountains = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "mountains");
        plateaus = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetrySettings, "plateaus");
        ramps = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "ramps");

        land.randomize(landDensity).smooth(2, .75f);

        if (random.nextBoolean()) {
            mountains.progressiveWalk((int) (mountainDensity * 160 / terrainSymmetry.getNumSymPoints()), mapSize / 16);
        } else {
            mountains.randomWalk((int) (mountainDensity * 160 / terrainSymmetry.getNumSymPoints()), mapSize / 16);
        }
        mountains.setSize(mapSize / 4).erode(.5f, symmetrySettings.getTerrainSymmetry(), 4).grow(.5f, symmetrySettings.getTerrainSymmetry(), 6);
        plateaus.randomize(plateauDensity).smooth(4);

        land.setSize(mapSize / 4).erode(.5f, symmetrySettings.getTerrainSymmetry(), mapSize / 256).grow(.5f, symmetrySettings.getTerrainSymmetry(), mapSize / 256);
        plateaus.setSize(mapSize / 4).erode(.5f, symmetrySettings.getTerrainSymmetry(), mapSize / 256).grow(.5f, symmetrySettings.getTerrainSymmetry(), mapSize / 64);

        land.setSize(mapSize + 1).smooth(8, .9f);
        mountains.setSize(mapSize + 1);
        plateaus.setSize(mapSize + 1).smooth(12, .1f);

        spawnPlateauMask.shrink(mapSize / 4).erode(.5f, symmetrySettings.getSpawnSymmetry(), 4).grow(.5f, symmetrySettings.getSpawnSymmetry(), 8);
        spawnPlateauMask.erode(.5f, symmetrySettings.getSpawnSymmetry()).setSize(mapSize + 1).smooth(4);

        spawnLandMask.shrink(mapSize / 4).erode(.25f, symmetrySettings.getSpawnSymmetry(), 4).grow(.5f, symmetrySettings.getSpawnSymmetry(), 6);
        spawnLandMask.erode(.5f, symmetrySettings.getSpawnSymmetry()).setSize(mapSize + 1).smooth(4);

        plateaus.minus(spawnLandMask).combine(spawnPlateauMask).removeAreasSmallerThan(512);
        land.combine(spawnLandMask).combine(spawnPlateauMask);

        boolean fillLandGaps = (random.nextFloat() < (landDensity - LAND_DENSITY_MIN) / LAND_DENSITY_RANGE);
        int fillSize = map.getSize() / 16;

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
            land.widenGaps((int) (fillSize * 2f));
        }

        land.removeAreasSmallerThan(mapSize * mapSize / 256);

        mountains.minus(spawnLandMask);

        plateaus.intersect(land).fillGaps(fillSize / 2).minus(spawnLandMask).combine(spawnPlateauMask).removeAreasSmallerThan(mapSize * mapSize / 256);
        land.combine(plateaus).combine(spawnLandMask).combine(spawnPlateauMask);

        mountains.smooth(8).intersect(land).minus(ramps);
        if (mountainDensity < .25) {
            mountains.widenGaps(12);
        } else if (mountainDensity < .5) {
            mountains.widenGaps(16);
        } else if (mountainDensity < .75) {
            mountains.acid(.0001f / terrainSymmetry.getNumSymPoints(), 24).widenGaps(16);
        } else {
            mountains.acid(.00025f / terrainSymmetry.getNumSymPoints(), 24).widenGaps(16);
        }
        mountains.removeAreasSmallerThan(128).smooth(8, .5f);

        ConcurrentBinaryMask landOutline = land.copy().outline().minus(plateaus.copy().inflate(1));

        ConcurrentBinaryMask shoreLine = landOutline.copy();

        shoreLine.setSize(mapSize / 4).erode(.75f, symmetrySettings.getSpawnSymmetry()).grow(.5f, symmetrySettings.getSpawnSymmetry(), 4).setSize(mapSize + 1).erode(.25f, symmetrySettings.getSpawnSymmetry(), 2);
        shoreLine.combine(landOutline.copy().flipValues(random.nextFloat() * .01f).grow(.5f, symmetrySettings.getSpawnSymmetry(), 18)).minus(ramps).smooth(2, .75f);

        land.combine(shoreLine);


        hills = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "hills");
        valleys = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "valleys");

        hills.randomWalk(random.nextInt(5) + 3, random.nextInt(500) + 350).setSize(mapSize + 1).smooth(10, .25f).intersect(land.copy().deflate(8)).minus(plateaus).minus(ramps).minus(spawnLandMask);
        valleys.randomWalk(random.nextInt(5) + 3, random.nextInt(500) + 350).setSize(mapSize + 1).smooth(10, .25f).intersect(plateaus.copy().deflate(4)).minus(ramps).minus(spawnPlateauMask);
    }

    private void setupHeightmapPipeline() {
        heightmapBase = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapBase");
        ConcurrentFloatMask heightmapValleys = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapValleys");
        ConcurrentFloatMask heightmapHills = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapHills");
        ConcurrentFloatMask heightmapPlateaus = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapPlateaus");
        ConcurrentFloatMask heightmapMountains = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapMountains");
        ConcurrentFloatMask heightmapLand = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapMountains");

        int length = Brushes.goodBrushes.size();
        String brush1 = Brushes.goodBrushes.get(random.nextInt(length));
        String brush2 = Brushes.goodBrushes.get(random.nextInt(length));
        String brush3 = Brushes.goodBrushes.get(random.nextInt(length));
        String brush4 = Brushes.goodBrushes.get(random.nextInt(length));

        heightmapMountains.useBrushRepeatedlyCenteredWithinAreaToDensity(mountains.deflate(2), brush3, 32, .30f, 5f).smooth(1);

        ConcurrentBinaryMask paintedMountains = new ConcurrentBinaryMask(heightmapMountains, PLATEAU_HEIGHT / 2, random.nextLong(), "paintedMountains");

        plateaus.combine(paintedMountains.intersect(plateaus.copy().inflate(32)));

        heightmapPlateaus.useBrushRepeatedlyCenteredWithinAreaToDensity(plateaus.deflate(8), brush1, 42, .16f, 14f).clampMax(PLATEAU_HEIGHT);
        heightmapValleys.useBrushRepeatedlyCenteredWithinAreaToDensity(valleys, brush2, 24, .28f, -0.25f)
                .clampMin(VALLEY_FLOOR).smooth(2);
        heightmapHills.useBrushRepeatedlyCenteredWithinAreaToDensity(hills, brush4, 24, .28f, 0.25f)
                .smooth(2);

        ConcurrentBinaryMask paintedPlateaus = new ConcurrentBinaryMask(heightmapPlateaus, PLATEAU_HEIGHT / 2, random.nextLong(), "paintedPlateaus");

        ConcurrentBinaryMask spawnRamps = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "spawnRamps");
        ramps.combine(paintedPlateaus.copy().outline()).minus(paintedMountains.copy().inflate(8)).flipValues(rampDensity * .004f + .002f).inflate(12);
        spawnRamps.combine(spawnLandMask.copy().outline()).combine(spawnPlateauMask.copy().outline()).inflate(36).intersect(paintedPlateaus.copy().outline()).flipValues(.01f).inflate(12);
        ramps.combine(spawnRamps).fillGaps(24).smooth(8, .25f);

        land.combine(paintedPlateaus);

        heightmapBase.addDistance(land, -.5f).clampMin(OCEAN_FLOOR).smooth(4, land.copy().invert().deflate(4));

        heightmapLand.add(heightmapHills).add(heightmapValleys).add(heightmapMountains).setValueInArea(LAND_HEIGHT, spawnLandMask).add(heightmapPlateaus).setValueInArea(PLATEAU_HEIGHT, spawnPlateauMask);
        heightmapLand.smooth(24, ramps).smooth(16, ramps.copy().inflate(10)).smooth(4, ramps.copy().inflate(14)).smooth(2, ramps.copy().inflate(18));

        heightmapBase.add(heightmapLand);
        heightmapBase.smooth(4, spawnLandMask.copy().combine(spawnPlateauMask).inflate(4));

        heightmapBase.addAll(waterHeight + LAND_HEIGHT).addGaussianNoise(.025f).clampMin(0f).clampMax(256f);

        slope = heightmapBase.copy().supcomGradient();

        impassable = new ConcurrentBinaryMask(slope, 1f, random.nextLong(), "impassable");
        unbuildable = new ConcurrentBinaryMask(slope, .5f, random.nextLong(), "unbuildable");
        ConcurrentBinaryMask notFlat = new ConcurrentBinaryMask(slope, .1f, random.nextLong(), "notFlat");

        unbuildable.combine(ramps.copy().intersect(notFlat));
        impassable.inflate(2);

        passable = new ConcurrentBinaryMask(impassable, random.nextLong(), "passable").invert();
        passableLand = new ConcurrentBinaryMask(land, random.nextLong(), "passableLand");
        passableWater = new ConcurrentBinaryMask(land, random.nextLong(), "passableWater").invert();

        passable.fillEdge(8, false);
        passableLand.intersect(passable);
        passableWater.deflate(16).fillEdge(8, false);
    }

    private void setupResourcePipeline() {
        resourceMask = new ConcurrentBinaryMask(land, random.nextLong(), "resource");
        waterResourceMask = new ConcurrentBinaryMask(land, random.nextLong(), "waterResource").invert();
        plateauResourceMask = new ConcurrentBinaryMask(land, random.nextLong(), "plateauResource");

        resourceMask.minus(unbuildable).deflate(8);
        resourceMask.fillEdge(16, false).fillCenter(24, false);
        waterResourceMask.minus(unbuildable).deflate(8).fillEdge(16, false).fillCenter(24, false);
        plateauResourceMask.combine(resourceMask).intersect(plateaus).fillEdge(16, false).fillCenter(24, false);
    }

    private void setupTexturePipeline() {
        ConcurrentBinaryMask flat = new ConcurrentBinaryMask(slope, .2f, random.nextLong(), "flat").invert();
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
        ConcurrentBinaryMask slopes = new ConcurrentBinaryMask(slope, .3f, random.nextLong(), "slopes");
        ConcurrentBinaryMask accentSlopes = new ConcurrentBinaryMask(slope, .55f, random.nextLong(), "accentSlopes").invert();
        ConcurrentBinaryMask steepHills = new ConcurrentBinaryMask(slope, .55f, random.nextLong(), "steepHills");
        ConcurrentBinaryMask rock = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "rock");
        ConcurrentBinaryMask accentRock = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "accentRock");
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
        accentGroundTexture.init(accentGround, 0, 1).smooth(8).add(accentGround, .65f).smooth(4).add(accentGround, .5f).smooth(1).clampMax(1f).threshold(.1f).smooth(2);
        accentPlateauTexture.init(accentPlateau, 0, 1).smooth(12).add(accentPlateau, .65f).smooth(6).add(accentPlateau, .5f).smooth(2).clampMax(1f).threshold(.1f).smooth(2);
        slopesTexture.init(slopes, 0, 1).smooth(8).add(slopes, .65f).smooth(4).add(slopes, .5f).smooth(1).clampMax(1f);
        accentSlopesTexture.init(accentSlopes, 0, 1).smooth(8).add(accentSlopes, .65f).smooth(4).add(accentSlopes, .5f).smooth(1).clampMax(1f);
        steepHillsTexture.init(steepHills, 0, 1).smooth(8).clampMax(0.35f).add(steepHills, .65f).smooth(4).clampMax(0.65f).add(steepHills, .5f).smooth(1).clampMax(1f);
        rockTexture.init(rock, 0, 1).smooth(8).clampMax(0.2f).add(rock, .65f).smooth(4).clampMax(0.3f).add(rock, .5f).smooth(4).add(rock, 1f).smooth(2).clampMax(1f);
        accentRockTexture.init(accentRock, 0, 1).smooth(8).add(accentRock, .65f).smooth(4).add(accentRock, .5f).smooth(1).clampMax(1f);
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
                civReclaimMask.randomize(.005f).setSize(mapSize + 1).intersect(land.copy().minus(unbuildable).minus(ramps).deflate(24)).fillCenter(32, false).fillEdge(64, false);
            } else {
                civReclaimMask.setSize(mapSize + 1);
                baseMask.randomize(.005f).setSize(mapSize + 1).intersect(land.copy().minus(unbuildable).minus(ramps).deflate(24)).fillCenter(32, false).fillEdge(32, false).minus(civReclaimMask.copy().inflate(16));
            }
        } else {
            civReclaimMask.setSize(mapSize + 1);
            baseMask.setSize(mapSize + 1);
        }
        allBaseMask.combine(baseMask.copy().inflate(24)).combine(civReclaimMask.copy().inflate(24));

        cliffRockMask.randomize(.5f).setSize(mapSize + 1).intersect(impassable).grow(.5f, symmetrySettings.getSpawnSymmetry(), 4).minus(plateaus.copy().outline()).intersect(land);
        fieldStoneMask.randomize(reclaimDensity * .001f).setSize(mapSize + 1).intersect(land).minus(impassable).fillEdge(10, false);
        treeMask.randomize(.2f).setSize(mapSize / 4).inflate(2).erode(.5f, symmetrySettings.getSpawnSymmetry()).smooth(4, .75f).erode(.5f, symmetrySettings.getSpawnSymmetry());
        treeMask.setSize(mapSize + 1).intersect(land.copy().deflate(8)).minus(impassable.copy().inflate(2)).deflate(2).fillEdge(8, false).smooth(4, .25f);
        largeRockFieldMask.randomize(reclaimDensity * .00075f).fillEdge(32, false).grow(.5f, symmetrySettings.getSpawnSymmetry(), 6).setSize(mapSize + 1).intersect(land).minus(impassable);
        smallRockFieldMask.randomize(reclaimDensity * .002f).fillEdge(16, false).grow(.5f, symmetrySettings.getSpawnSymmetry(), 2).setSize(mapSize + 1).intersect(land).minus(impassable);
    }

    private void setupWreckPipeline() {
        t1LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t1LandWreck");
        t2LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t2LandWreck");
        t3LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t3LandWreck");
        t2NavyWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t2NavyWreck");
        navyFactoryWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "navyFactoryWreck");
        allWreckMask = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "allWreck");

        t1LandWreckMask.randomize(reclaimDensity * .0035f).setSize(mapSize + 1).intersect(land).inflate(1).fillEdge(20, false);
        t2LandWreckMask.randomize(reclaimDensity * .0025f).setSize(mapSize + 1).intersect(land).minus(t1LandWreckMask).fillEdge(64, false);
        t3LandWreckMask.randomize(reclaimDensity * .00025f).setSize(mapSize + 1).intersect(land).minus(t1LandWreckMask).minus(t2LandWreckMask).fillEdge(mapSize / 8, false);
        navyFactoryWreckMask.randomize(reclaimDensity * .005f).setSize(mapSize + 1).minus(land.copy().inflate(16)).fillEdge(20, false);
        t2NavyWreckMask.randomize(reclaimDensity * .005f).setSize(mapSize + 1).intersect(land.copy().inflate(4).outline()).fillEdge(20, false);
        allWreckMask.combine(t1LandWreckMask).combine(t2LandWreckMask).combine(t3LandWreckMask).combine(t2NavyWreckMask).inflate(2);
    }

    private void generateExclusionMasks() {
        noProps = new BinaryMask(unbuildable.getFinalMask(), null);

        for (int i = 0; i < map.getSpawnCount(); i++) {
            noProps.fillCircle(map.getSpawn(i).getPosition(), 30, true);
        }
        for (int i = 0; i < map.getMexCount(); i++) {
            noProps.fillCircle(map.getMex(i).getPosition(), 1, true);
        }
        for (int i = 0; i < map.getHydroCount(); i++) {
            noProps.fillCircle(map.getHydro(i).getPosition(), 8, true);
        }

        noProps.combine(allBaseMask.getFinalMask());

        noBases = new BinaryMask(unbuildable.getFinalMask(), null);

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

        for (int i = 0; i < map.getSpawnCount(); i++) {
            noCivs.fillCircle(map.getSpawn(i).getPosition(), 96, true);
        }
        for (int i = 0; i < map.getMexCount(); i++) {
            noCivs.fillCircle(map.getMex(i).getPosition(), 32, true);
        }
        for (int i = 0; i < map.getHydroCount(); i++) {
            noCivs.fillCircle(map.getHydro(i).getPosition(), 32, true);
        }

        noWrecks = new BinaryMask(unbuildable.getFinalMask(), null);

        noWrecks.combine(allBaseMask.getFinalMask()).fillCenter(16, true);

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
