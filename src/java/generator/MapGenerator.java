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

    public static final String VERSION;
    public static final BaseEncoding NAME_ENCODER = BaseEncoding.base32().omitPadding().lowerCase();
    public static final float LAND_DENSITY_MIN = .8f;
    public static final float LAND_DENSITY_MAX = .9f;
    public static final float LAND_DENSITY_RANGE = LAND_DENSITY_MAX - LAND_DENSITY_MIN;
    public static final float MOUNTAIN_DENSITY_MIN = 0f;
    public static final float MOUNTAIN_DENSITY_MAX = 1f;
    public static final float MOUNTAIN_DENSITY_RANGE = MOUNTAIN_DENSITY_MAX - MOUNTAIN_DENSITY_MIN;
    public static final float RAMP_DENSITY_MIN = 0f;
    public static final float RAMP_DENSITY_MAX = 1f;
    public static final float RAMP_DENSITY_RANGE = RAMP_DENSITY_MAX - RAMP_DENSITY_MIN;
    public static final float PLATEAU_DENSITY_MIN = .5f;
    public static final float PLATEAU_DENSITY_MAX = .7f;
    public static final float PLATEAU_DENSITY_RANGE = PLATEAU_DENSITY_MAX - PLATEAU_DENSITY_MIN;
    public static final float RECLAIM_DENSITY_MIN = 0f;
    public static final float RECLAIM_DENSITY_MAX = 1f;
    public static final float RECLAIM_DENSITY_RANGE = RECLAIM_DENSITY_MAX - RECLAIM_DENSITY_MIN;
    public static final float PLATEAU_HEIGHT = 8.5f;
    public static final float OCEAN_FLOOR = -17f;
    public static final float VALLEY_FLOOR = -5f;
    public static final float LAND_HEIGHT = 1f;
    private static final String BLANK_PREVIEW = "/images/generatedMapIcon.png";
    public static boolean DEBUG = false;

    static {
        String version = MapGenerator.class.getPackage().getImplementationVersion();
        VERSION = version != null ? version : "SNAPSHOT";
    }

    //read from cli args
    private String pathToFolder = ".";
    private String mapName = "debugMap";
    private long seed = new Random().nextLong();
    private Random random;
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
    private boolean optionsUsed = false;

    //masks used in generation
    private ConcurrentBinaryMask land;
    private ConcurrentBinaryMask mountains;
    private ConcurrentBinaryMask hills;
    private ConcurrentBinaryMask valleys;
    private ConcurrentBinaryMask plateaus;
    private ConcurrentBinaryMask ramps;
    private ConcurrentBinaryMask impassable;
    private ConcurrentBinaryMask unbuildable;
    private ConcurrentBinaryMask notFlat;
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

        tournamentStyle = arguments.containsKey("tournament-style") || arguments.containsKey("blind");
        blind = arguments.containsKey("blind");

        if (tournamentStyle) {
            generationTime = Instant.now().getEpochSecond();
        }

        if (arguments.containsKey("seed") && arguments.get("seed") != null) {
            seed = Long.parseLong(arguments.get("seed"));
        }

        if (arguments.containsKey("spawn-count") && arguments.get("spawn-count") != null) {
            spawnCount = Integer.parseInt(arguments.get("spawn-count"));
        }

        if (arguments.containsKey("map-size") && arguments.get("map-size") != null) {
            mapSize = Integer.parseInt(arguments.get("map-size"));
        }

        randomizeOptions();


        if (!tournamentStyle && !blind) {
            if (arguments.containsKey("land-density") && arguments.get("land-density") != null) {
                float inLandDensity = Float.parseFloat(arguments.get("land-density"));
                landDensity = StrictMath.round(inLandDensity * 127f) / 127f;
                optionsUsed = true;
            }

            if (arguments.containsKey("plateau-density") && arguments.get("plateau-density") != null) {
                float inPlateauDensity = Float.parseFloat(arguments.get("plateau-density"));
                plateauDensity = StrictMath.round(inPlateauDensity * 127f) / 127f;
                optionsUsed = true;
            }

            if (arguments.containsKey("mountain-density") && arguments.get("mountain-density") != null) {
                float inMountainDensity = Float.parseFloat(arguments.get("mountain-density"));
                mountainDensity = StrictMath.round(inMountainDensity * 127f) / 127f;
                optionsUsed = true;
            }

            if (arguments.containsKey("ramp-density") && arguments.get("ramp-density") != null) {
                float inRampDensity = Float.parseFloat(arguments.get("ramp-density"));
                rampDensity = StrictMath.round(inRampDensity * 127f) / 127f;
                optionsUsed = true;
            }

            if (arguments.containsKey("reclaim-density") && arguments.get("reclaim-density") != null) {
                float inReclaimDensity = Float.parseFloat(arguments.get("reclaim-density"));
                reclaimDensity = StrictMath.round(inReclaimDensity * 127f) / 127f;
                optionsUsed = true;
            }

            if (arguments.containsKey("mex-count") && arguments.get("mex-count") != null) {
                mexCount = Integer.parseInt(arguments.get("mex-count"));
                optionsUsed = true;
            }

            if (arguments.containsKey("biome") && arguments.get("biome") != null) {
                biome = Biomes.getBiomeByName(arguments.get("biome"));
                optionsUsed = true;
            }
        }

        generateMapName();
    }

    private void parseMapName() {
        if (!mapName.startsWith("neroxis_map_generator")) {
            throw new IllegalArgumentException("Map name is not a generated map");
        }
        String[] args = mapName.split("_");
        if (args.length < 4) {
            throw new RuntimeException("Version not specified");
        }
        if (args.length < 5) {
            throw new RuntimeException("Seed not specified");
        }
        String version = args[3];
        if (!VERSION.equals(version)) {
            throw new RuntimeException("Wrong generator version: " + version);
        }

        byte[] optionBytes = new byte[0];

        String seedString = args[4];
        try {
            seed = Long.parseLong(seedString);
        } catch (NumberFormatException nfe) {
            byte[] seedBytes = NAME_ENCODER.decode(seedString);
            ByteBuffer seedWrapper = ByteBuffer.wrap(seedBytes);
            seed = seedWrapper.getLong();
        }

        if (args.length >= 6) {
            String optionString = args[5];
            optionBytes = NAME_ENCODER.decode(optionString);
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

        parseOptions(optionBytes);
    }

    private void randomizeOptions() {
        random = new Random(seed ^ generationTime);

        landDensity = random.nextInt(127) / 127f;
        plateauDensity = random.nextInt(127) / 127f;
        mountainDensity = random.nextInt(127) / 127f;
        rampDensity = random.nextInt(127) / 127f;
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
                if (spawnSymmetry == Symmetry.POINT4) {
                    teams = new ArrayList<>(Arrays.asList(Symmetry.QUAD, Symmetry.DIAG));
                } else {
                    teams = new ArrayList<>(Arrays.asList(Symmetry.X, Symmetry.Z, Symmetry.XZ, Symmetry.ZX));
                }
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
            landDensity = optionBytes[2] / 127f;
        }
        if (optionBytes.length > 3) {
            plateauDensity = optionBytes[3] / 127f;
        }
        if (optionBytes.length > 4) {
            mountainDensity = optionBytes[4] / 127f;
        }
        if (optionBytes.length > 5) {
            rampDensity = optionBytes[5] / 127f;
        }
        if (optionBytes.length > 6) {
            reclaimDensity = optionBytes[6] / 127f;
        }
        if (optionBytes.length > 7) {
            mexCount = optionBytes[7];
        }
        if (optionBytes.length > 8) {
            biome = Biomes.list.get(optionBytes[8]);
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
        byte[] optionArray;
        if (optionsUsed) {
            optionArray = new byte[]{(byte) spawnCount,
                    (byte) (mapSize / 64),
                    (byte) StrictMath.round(landDensity * 127f),
                    (byte) StrictMath.round(plateauDensity * 127f),
                    (byte) StrictMath.round(mountainDensity * 127f),
                    (byte) StrictMath.round(rampDensity * 127f),
                    (byte) StrictMath.round(reclaimDensity * 127f),
                    (byte) mexCount,
                    (byte) Biomes.list.indexOf(Biomes.getBiomeByName(biome.getName()))};
        } else {
            optionArray = new byte[]{(byte) spawnCount,
                    (byte) (mapSize / 64)};
        }
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

        final int spawnSize = 36;
        final int hydroCount = spawnCount >= 4 ? spawnCount + random.nextInt(spawnCount / 4) * 2 : spawnCount;
        int mexSpacing = mapSize / 10;
        mexSpacing *= StrictMath.min(StrictMath.max(36f / (mexCount * spawnCount), .5f), 1.5f);
        hasCivilians = random.nextBoolean();
        enemyCivilians = random.nextBoolean();
        map = new SCMap(mapSize, spawnCount, mexCount * spawnCount, hydroCount, biome);
        waterHeight = biome.getWaterSettings().getElevation();

        SpawnGenerator spawnGenerator = new SpawnGenerator(map, random.nextLong(), spawnSize);
        MexGenerator mexGenerator = new MexGenerator(map, random.nextLong(), mexSpacing);
        HydroGenerator hydroGenerator = new HydroGenerator(map, random.nextLong());
        PropGenerator propGenerator = new PropGenerator(map, random.nextLong());
        DecalGenerator decalGenerator = new DecalGenerator(map, random.nextLong());
        UnitGenerator unitGenerator = new UnitGenerator(map, random.nextLong());
        AIMarkerGenerator aiMarkerGenerator = new AIMarkerGenerator(map, random.nextLong());

        spawnSeparation = switch (terrainSymmetry) {
            case Z, X -> StrictMath.max(StrictMath.max(random.nextInt(map.getSize() / 4 - map.getSize() / 32) + map.getSize() / 32, map.getSize() / spawnCount), 48);
            case NONE -> mapSize / spawnCount * 2;
            default -> StrictMath.max(random.nextInt(map.getSize() / 4 - map.getSize() / 32) + map.getSize() / 32, 48);
        };

        BinaryMask[] spawnMasks = spawnGenerator.generateSpawns(spawnSeparation, symmetrySettings, plateauDensity);
        spawnLandMask = new ConcurrentBinaryMask(spawnMasks[0], random.nextLong(), "spawnsLand");
        spawnPlateauMask = new ConcurrentBinaryMask(spawnMasks[1], random.nextLong(), "spawnsPlateau");

        setupPipeline();

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
            Pipeline.await(resourceMask, plateaus, land, ramps, impassable, unbuildable, allWreckMask, waterResourceMask);
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
            propGenerator.generateProps(treeMask.getFinalMask().minus(noProps), biome.getPropMaterials().getTreeGroups(), 3f, 7f);
            propGenerator.generateProps(cliffRockMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), .5f, 3f);
            propGenerator.generateProps(largeRockFieldMask.getFinalMask().minus(noProps.copy().inflate(16)), biome.getPropMaterials().getRocks(), .5f, 3.5f);
            propGenerator.generateProps(smallRockFieldMask.getFinalMask().minus(noProps.copy().inflate(16)), biome.getPropMaterials().getRocks(), .5f, 3.5f);
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
            unitGenerator.generateUnits(t1LandWreckMask.getFinalMask().minus(noWrecks), UnitGenerator.T1_Land, army17, army17Wreckage, 1f, 4f);
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

    private void setupPipeline() {
        setupTerrainPipeline();
        setupHeightmapPipeline();
        setupTexturePipeline();
        setupPropPipeline();
        setupWreckPipeline();
        setupResourcePipeline();
    }

    private void setupTerrainPipeline() {
        boolean landPathed = false;
        if (landDensity >= .75f && mountainDensity > .5f && random.nextBoolean()) {
            allLandInit();
            pathMountainInit();
        } else {
            if (!(landDensity < .25f && mapSize < 1024) && landDensity < .75 && random.nextBoolean()) {
                pathLandInit();
                landPathed = true;
            } else {
                smoothLandInit();
            }
            walkMountainInit();
        }

        if (plateauDensity > .5 && random.nextBoolean()) {
            pathPlateauInit();
        } else {
            smoothPlateauInit();
        }

        spawnPlateauMask.setSize(mapSize / 4).erode(.5f, SymmetryType.SPAWN, 4).grow(.5f, SymmetryType.SPAWN, 12);
        spawnPlateauMask.erode(.5f, SymmetryType.SPAWN).setSize(mapSize + 1).smooth(4);

        spawnLandMask.setSize(mapSize / 4).erode(.25f, SymmetryType.SPAWN, mapSize / 128).grow(.5f, SymmetryType.SPAWN, mapSize / 64);
        spawnLandMask.erode(.5f, SymmetryType.SPAWN).setSize(mapSize + 1).smooth(4);

        plateaus.minus(spawnLandMask).combine(spawnPlateauMask).removeAreasSmallerThan(512);
        land.combine(spawnLandMask).combine(spawnPlateauMask);

        plateaus.minus(spawnLandMask).combine(spawnPlateauMask);
        land.combine(spawnLandMask).combine(spawnPlateauMask);
        if (!landPathed && mapSize > 512) {
            land.widenGaps(mapSize / 12).combine(spawnLandMask).combine(spawnPlateauMask).setSize(mapSize / 4)
                    .erode(.5f, SymmetryType.SPAWN, 8).setSize(mapSize + 1).smooth(8);
        } else if (!landPathed) {
            land.fillGaps(mapSize / 16).grow(.5f, SymmetryType.SPAWN, 8).smooth(2);
        }

        land.removeAreasSmallerThan(mapSize * mapSize / 256);

        mountains.minus(spawnLandMask);

        plateaus.intersect(land).fillGaps(mapSize / 16).minus(spawnLandMask).combine(spawnPlateauMask).removeAreasSmallerThan(mapSize * mapSize / 256);
        land.combine(plateaus).combine(spawnLandMask).combine(spawnPlateauMask);

        mountains.smooth(8, .75f);
        mountains.intersect(landPathed || landDensity < .25f ? land.copy().deflate(24) : land);
        mountains.removeAreasSmallerThan(256);

        hills = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "hills");
        valleys = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "valleys");

        hills.randomWalk(random.nextInt(3) + 1, random.nextInt(mapSize / 2)).setSize(mapSize + 1).smooth(10, .25f).intersect(land.copy().deflate(8)).minus(plateaus.copy().inflate(8)).minus(spawnLandMask);
        valleys.randomWalk(random.nextInt(3) + 1, random.nextInt(mapSize / 2)).setSize(mapSize + 1).smooth(10, .25f).intersect(plateaus.copy().deflate(8)).minus(spawnPlateauMask);
    }

    private void allLandInit() {
        land = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "land").invert();
    }

    private void smoothLandInit() {
        float scaledLandDensity = landDensity * LAND_DENSITY_RANGE + LAND_DENSITY_MIN;
        land = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetrySettings, "land");

        land.randomize(scaledLandDensity).smooth(2, .75f).erode(.5f, SymmetryType.TERRAIN, mapSize / 256);
        land.setSize(mapSize / 4).grow(.5f, SymmetryType.TERRAIN, mapSize / 128);
        land.setSize(mapSize + 1).smooth(8, .75f);
    }

    private void pathLandInit() {
        float maxStepSize = mapSize / 128f;
        float maxAngleError = (float) (StrictMath.PI * 4f / 5f);
        float inertia = random.nextFloat() * .45f + .2f;
        float distanceThreshold = maxStepSize / 2f;
        int maxNumSteps = mapSize * mapSize;
        int numWalkersPerPlayer = 2;
        int numWalkers = (int) (8 * landDensity / .75f + 8) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = (int) (mapSize / 16 * (5 * (random.nextFloat() + (1 - landDensity / .75f)) / 2f + 1));
        land = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "land");

        map.getSpawns().forEach(spawn -> {
            for (int i = 0; i < numWalkersPerPlayer; i++) {
                Vector2f start = new Vector2f(spawn.getPosition());
                Vector2f end = new Vector2f(random.nextInt(mapSize + 1 - bound * 2) + bound, random.nextInt(mapSize + 1 - bound * 2) + bound);
                land.path(start, end, maxStepSize, maxAngleError, inertia, distanceThreshold, maxNumSteps, SymmetryType.TERRAIN);
            }
        });

        for (int i = 0; i < numWalkers; i++) {
            Vector2f start = new Vector2f(random.nextInt(mapSize + 1 - bound * 2) + bound, random.nextInt(mapSize + 1 - bound * 2) + bound);
            Vector2f end = new Vector2f(random.nextInt(mapSize + 1 - bound * 2) + bound, random.nextInt(mapSize + 1 - bound * 2) + bound);
            land.path(start, end, maxStepSize, maxAngleError, inertia, distanceThreshold, maxNumSteps, SymmetryType.TERRAIN);
        }
        land.inflate(mapSize / 256f).setSize(mapSize / 4).grow(.5f, SymmetryType.TERRAIN, 4).setSize(mapSize + 1).smooth(6);
    }

    private void smoothPlateauInit() {
        float scaledPlateauDensity = plateauDensity * PLATEAU_DENSITY_RANGE + PLATEAU_DENSITY_MIN;
        plateaus = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetrySettings, "plateaus");

        plateaus.randomize(scaledPlateauDensity).smooth(2, .75f).erode(.5f, SymmetryType.TERRAIN, mapSize / 256);
        plateaus.setSize(mapSize / 4).grow(.5f, SymmetryType.TERRAIN, mapSize / 64);
        plateaus.setSize(mapSize + 1).smooth(8, .75f);
    }

    private void pathPlateauInit() {
        float maxStepSize = mapSize / 128f;
        float maxAngleError = (float) (StrictMath.PI * 4f / 5f);
        float inertia = random.nextFloat() * .35f + .25f;
        float distanceThreshold = maxStepSize / 2f;
        int maxNumSteps = mapSize * mapSize;
        int numWalkers = (int) (4 * (plateauDensity - .5f) / .5f + 4) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        plateaus = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "plateaus");

        for (int i = 0; i < numWalkers; i++) {
            Vector2f start = new Vector2f(random.nextInt(mapSize + 1), random.nextInt(mapSize + 1));
            Vector2f end = new Vector2f(random.nextInt(mapSize + 1), random.nextInt(mapSize + 1));
            plateaus.path(start, end, maxStepSize, maxAngleError, inertia, distanceThreshold, maxNumSteps, SymmetryType.TERRAIN);
        }
        plateaus.inflate(mapSize / 256f).setSize(mapSize / 4).grow(.5f, SymmetryType.TERRAIN, 4).smooth(4).setSize(mapSize + 1).smooth(12);
    }

    private void walkMountainInit() {
        float scaledMountainDensity = mountainDensity * MOUNTAIN_DENSITY_RANGE + MOUNTAIN_DENSITY_MIN;
        if (mapSize < 512) {
            scaledMountainDensity = StrictMath.max(scaledMountainDensity - .25f, 0);
        }

        mountains = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "mountains");

        if (random.nextBoolean()) {
            mountains.progressiveWalk((int) (scaledMountainDensity * 120 / terrainSymmetry.getNumSymPoints()), mapSize / 16);
        } else {
            mountains.randomWalk((int) (scaledMountainDensity * 120 / terrainSymmetry.getNumSymPoints()), mapSize / 16);
        }
        mountains.setSize(mapSize / 4).erode(.5f, SymmetryType.TERRAIN, 4).grow(.5f, SymmetryType.TERRAIN, 6);
        mountains.setSize(mapSize + 1);


        if (mountainDensity > .5f) {
            float maxStepSize = mapSize / 256f;
            float maxAngleError = (float) (StrictMath.PI * 3f / 5f);
            float inertia = .5f;
            float distanceThreshold = maxStepSize / 2f;
            int maxNumSteps = mapSize * mapSize / 4;
            ConcurrentBinaryMask connections = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "connections");

            map.getSpawns().forEach(startSpawn -> {
                ArrayList<Spawn> otherSpawns = new ArrayList<>(map.getSpawns());
                otherSpawns.remove(startSpawn);
                for (int i = 0; i < 1; i++) {
                    Spawn endSpawn = otherSpawns.get(random.nextInt(otherSpawns.size()));
                    Vector2f start = new Vector2f(startSpawn.getPosition());
                    Vector2f end = new Vector2f(endSpawn.getPosition());
                    connections.path(start, end, maxStepSize, maxAngleError, inertia, distanceThreshold, maxNumSteps, SymmetryType.SPAWN);
                }
            });
            connections.grow(.5f, SymmetryType.SPAWN, 8).smooth(6);

            mountains.minus(connections);
        }
    }

    private void pathMountainInit() {
        float maxStepSize = mapSize / 128f;
        float maxAngleError = (float) (StrictMath.PI * 4f / 5f);
        float inertiaSpawn = .75f;
        float inertiaPath = random.nextFloat() * .45f + .25f;
        float distanceThreshold = maxStepSize / 2f;
        int maxNumSteps = mapSize * mapSize;
        int numWalkers = (int) (32 + 32 * (1 - (mountainDensity - .5f) / .5f)) / symmetrySettings.getTerrainSymmetry().getNumSymPoints();
        int bound = (int) (mapSize / 8 * (random.nextFloat() + (mountainDensity - .5f) / .5f) / 2f);
        mountains = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "mountains");
        ConcurrentBinaryMask connections = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "connections");

        map.getSpawns().forEach(startSpawn -> {
            ArrayList<Spawn> otherSpawns = new ArrayList<>(map.getSpawns());
            otherSpawns.remove(startSpawn);
            Spawn endSpawn = otherSpawns.get(random.nextInt(otherSpawns.size()));
            Vector2f start = new Vector2f(startSpawn.getPosition());
            Vector2f end = new Vector2f(endSpawn.getPosition());
            connections.path(start, end, maxStepSize, maxAngleError, inertiaSpawn, distanceThreshold, maxNumSteps, SymmetryType.TERRAIN);
        });

        for (int i = 0; i < numWalkers; i++) {
            Vector2f start = new Vector2f(random.nextInt(mapSize + 1 - bound * 2) + bound, random.nextInt(mapSize + 1 - bound * 2) + bound);
            Vector2f end = new Vector2f(random.nextInt(mapSize + 1 - bound * 2) + bound, random.nextInt(mapSize + 1 - bound * 2) + bound);
            connections.path(start, end, maxStepSize, maxAngleError, inertiaPath, distanceThreshold, maxNumSteps, SymmetryType.TERRAIN);
        }
        connections.setSize(mapSize / 4).grow(.5f, SymmetryType.TERRAIN, 4).setSize(mapSize + 1).smooth(8);

        mountains.invert().minus(connections);
    }

    private void initRamps() {
        float maxStepSize = mapSize / 128f;
        float maxAngleError = (float) (StrictMath.PI / 2f);
        float inertia = random.nextFloat() * .25f + .25f;
        float distanceThreshold = maxStepSize / 2f;
        int maxNumSteps = mapSize * mapSize;
        int numWalkersPerPlayer = (int) (rampDensity * 8 + 2);
        int numWalkers = (int) (rampDensity * 8 + 8) / symmetrySettings.getTerrainSymmetry().getNumSymPoints() + spawnCount / 4;
        int bound = mapSize / 32;
        ramps = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "ramps");
        map.getSpawns().forEach(spawn -> {
            for (int i = 0; i < numWalkersPerPlayer; i++) {
                Vector2f start = new Vector2f(spawn.getPosition());
                Vector2f end = new Vector2f(random.nextInt(mapSize / 2) + start.x - mapSize / 4f,
                        random.nextInt(mapSize / 2) + start.y - mapSize / 4f);
                ramps.path(start, end, maxStepSize, maxAngleError, inertia, distanceThreshold, maxNumSteps, SymmetryType.TERRAIN);
            }
        });
        for (int i = 0; i < numWalkers; i++) {
            Vector2f start = new Vector2f(random.nextInt(mapSize + 1 - bound * 2) + bound, random.nextInt(mapSize + 1 - bound * 2) + bound);
            Vector2f end = new Vector2f(random.nextInt(mapSize + 1 - bound * 2) + bound, random.nextInt(mapSize + 1 - bound * 2) + bound);
            ramps.path(start, end, maxStepSize, maxAngleError, inertia, distanceThreshold, maxNumSteps, SymmetryType.TERRAIN);
        }
        ramps.inflate(distanceThreshold).intersect(plateaus.copy().outline())
                .minus(mountains.copy().inflate(8)).inflate(12).fillGaps(24).smooth(8, .25f);
    }

    private void setupHeightmapPipeline() {
        int length = Brushes.goodBrushes.size();
        String brush1 = Brushes.goodBrushes.get(random.nextInt(length));
        String brush2 = Brushes.goodBrushes.get(random.nextInt(length));
        String brush3 = Brushes.goodBrushes.get(random.nextInt(length));
        String brush4 = Brushes.goodBrushes.get(random.nextInt(length));
        String brush5 = Brushes.goodBrushes.get(random.nextInt(length));

        heightmapBase = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapBase");
        ConcurrentFloatMask heightmapValleys = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapValleys");
        ConcurrentFloatMask heightmapHills = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapHills");
        ConcurrentFloatMask heightmapPlateaus = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapPlateaus");
        ConcurrentFloatMask heightmapMountains = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapMountains");
        ConcurrentFloatMask heightmapLand = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapLand");
        ConcurrentFloatMask heightmapOcean = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapOcean");

        heightmapMountains.useBrushWithinAreaWithDensity(mountains, brush3, 32, .5f, 2.5f);

        ConcurrentBinaryMask paintedMountains = new ConcurrentBinaryMask(heightmapMountains, LAND_HEIGHT, random.nextLong(), "paintedMountains");

        plateaus.combine(paintedMountains.copy().intersect(plateaus.copy().inflate(32)));
        mountains.combine(paintedMountains);
        land.combine(paintedMountains);

        heightmapPlateaus.useBrushWithinAreaWithDensity(plateaus.deflate(4), brush1, 36, .24f, 24f).clampMax(PLATEAU_HEIGHT);
        heightmapValleys.useBrushWithinAreaWithDensity(valleys, brush2, 24, .36f, -0.35f)
                .clampMin(VALLEY_FLOOR);
        heightmapHills.useBrushWithinAreaWithDensity(hills, brush4, 24, .36f, 0.5f);

        ConcurrentBinaryMask paintedPlateaus = new ConcurrentBinaryMask(heightmapPlateaus, LAND_HEIGHT, random.nextLong(), "paintedPlateaus");

        land.combine(paintedPlateaus);
        plateaus.combine(paintedPlateaus);

        initRamps();

        ConcurrentBinaryMask water = land.copy().invert();
        ConcurrentBinaryMask deepWater = water.copy().deflate(32);

        heightmapOcean.addDistance(land, -.45f).clampMin(OCEAN_FLOOR).useBrushWithinAreaWithDensity(water.minus(deepWater), brush5, 16, .5f, .5f)
                .useBrushWithinAreaWithDensity(deepWater, brush5, 64, .0325f, 1f).clampMax(0).smooth(4, deepWater);

        ConcurrentBinaryMask paintedLand = new ConcurrentBinaryMask(heightmapOcean, 0, random.nextLong(), "paintedLand");

        land.combine(paintedLand);

        heightmapLand.add(heightmapHills).add(heightmapValleys).add(heightmapMountains).add(LAND_HEIGHT)
                .setValueInArea(LAND_HEIGHT, spawnLandMask).add(heightmapPlateaus).setValueInArea(PLATEAU_HEIGHT + LAND_HEIGHT, spawnPlateauMask)
                .smooth(1, spawnPlateauMask.copy().inflate(4)).smooth(24, ramps).smooth(16, ramps.copy().inflate(10))
                .smooth(4, ramps.copy().inflate(14)).smooth(2, ramps.copy().inflate(18));

        heightmapBase.add(heightmapOcean).smooth(1).add(heightmapLand);

        heightmapBase.add(waterHeight).addWhiteNoise(.05f).clampMin(0f).clampMax(256f);

        slope = heightmapBase.copy().supcomGradient();

        impassable = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "impassable");
        unbuildable = new ConcurrentBinaryMask(slope, .2f, random.nextLong(), "unbuildable");
        notFlat = new ConcurrentBinaryMask(slope, .05f, random.nextLong(), "notFlat");

        unbuildable.combine(ramps.copy().intersect(notFlat));
        impassable.inflate(2).combine(paintedMountains);

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

        resourceMask.minus(unbuildable).deflate(4);
        resourceMask.fillEdge(16, false).fillCenter(24, false);
        waterResourceMask.minus(unbuildable).deflate(8).fillEdge(16, false).fillCenter(24, false);
    }

    private void setupTexturePipeline() {
        ConcurrentBinaryMask flat = new ConcurrentBinaryMask(slope, .05f, random.nextLong(), "flat").invert();
        ConcurrentBinaryMask highGround = new ConcurrentBinaryMask(heightmapBase, waterHeight + PLATEAU_HEIGHT / 2f, random.nextLong(), "highGround");
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

        accentGround.minus(highGround).acid(.1f, 0).erode(.4f, SymmetryType.SPAWN).smooth(3, .75f);
        accentPlateau.acid(.05f, 0).erode(.85f, SymmetryType.SPAWN).smooth(2, .75f).acid(.6f, 0);
        slopes.intersect(land).flipValues(.95f).erode(.5f, SymmetryType.SPAWN).acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentSlopes.minus(flat).intersect(land).acid(.1f, 0).erode(.5f, SymmetryType.SPAWN).smooth(4, .75f).acid(.55f, 0);
        steepHills.acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentRock.acid(.2f, 0).erode(.3f, SymmetryType.SPAWN).acid(.2f, 0).smooth(2, .5f).intersect(rock);

        waterBeachTexture.init(land.copy().invert().inflate(12).minus(plateaus.copy().minus(ramps)), 0, 1).smooth(12);
        accentGroundTexture.init(accentGround, 0, 1).smooth(12).add(accentGround, .65f).smooth(8).add(accentGround, .5f).smooth(2).clampMax(1f).threshold(.1f).smooth(2);
        accentPlateauTexture.init(accentPlateau, 0, 1).smooth(12).add(accentPlateau, .65f).smooth(8).add(accentPlateau, .5f).smooth(2).clampMax(1f).threshold(.1f).smooth(2);
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

        cliffRockMask.randomize(reclaimDensity * .5f + .1f).setSize(mapSize + 1).intersect(impassable).grow(.5f, SymmetryType.SPAWN, 6).minus(plateaus.copy().outline().inflate(2)).minus(impassable).intersect(land);
        fieldStoneMask.randomize(reclaimDensity * .001f).setSize(mapSize + 1).intersect(land).minus(impassable).fillEdge(10, false);
        treeMask.randomize(reclaimDensity * .2f + .05f).setSize(mapSize / 4).inflate(2).erode(.5f, SymmetryType.SPAWN).smooth(4, .75f).erode(.5f, SymmetryType.SPAWN);
        treeMask.setSize(mapSize + 1).intersect(land.copy().deflate(8)).minus(impassable.copy().inflate(2)).deflate(2).fillEdge(8, false).minus(notFlat).smooth(4, .25f);
        largeRockFieldMask.randomize(reclaimDensity * .001f).fillEdge(32, false).grow(.5f, SymmetryType.SPAWN, 8).setSize(mapSize + 1).minus(notFlat).intersect(land).minus(impassable);
        smallRockFieldMask.randomize(reclaimDensity * .0025f).fillEdge(16, false).grow(.5f, SymmetryType.SPAWN, 4).setSize(mapSize + 1).minus(notFlat).intersect(land).minus(impassable);
    }

    private void setupWreckPipeline() {
        t1LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t1LandWreck");
        t2LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t2LandWreck");
        t3LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t3LandWreck");
        t2NavyWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t2NavyWreck");
        navyFactoryWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "navyFactoryWreck");
        allWreckMask = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "allWreck");

        t1LandWreckMask.randomize(reclaimDensity * .0025f).setSize(mapSize + 1).intersect(land).inflate(1).minus(impassable).fillEdge(20, false);
        t2LandWreckMask.randomize(reclaimDensity * .002f).setSize(mapSize + 1).intersect(land).minus(impassable).minus(t1LandWreckMask).fillEdge(64, false);
        t3LandWreckMask.randomize(reclaimDensity * .0004f).setSize(mapSize + 1).intersect(land).minus(impassable).minus(t1LandWreckMask).minus(t2LandWreckMask).fillEdge(mapSize / 8, false);
        navyFactoryWreckMask.randomize(reclaimDensity * .005f).setSize(mapSize + 1).minus(land.copy().inflate(16)).fillEdge(20, false).fillCenter(32, false);
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
