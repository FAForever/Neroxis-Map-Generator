package neroxis.generator;

import com.google.common.io.BaseEncoding;
import lombok.Getter;
import lombok.Setter;
import neroxis.biomes.Biome;
import neroxis.biomes.Biomes;
import neroxis.exporter.MapExporter;
import neroxis.exporter.SCMapExporter;
import neroxis.generator.mapstyles.MapStyle;
import neroxis.map.*;
import neroxis.util.*;

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
import java.util.stream.Collectors;

import static neroxis.util.ImageUtils.readImage;

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
    public static final float PLATEAU_DENSITY_MIN = .6f;
    public static final float PLATEAU_DENSITY_MAX = .7f;
    public static final float PLATEAU_DENSITY_RANGE = PLATEAU_DENSITY_MAX - PLATEAU_DENSITY_MIN;
    public static final float RECLAIM_DENSITY_MIN = 0f;
    public static final float RECLAIM_DENSITY_MAX = 1f;
    public static final float RECLAIM_DENSITY_RANGE = RECLAIM_DENSITY_MAX - RECLAIM_DENSITY_MIN;
    public static final float PLATEAU_HEIGHT = 6f;
    public static final float OCEAN_FLOOR = -16f;
    public static final float VALLEY_FLOOR = -5f;
    public static final float LAND_HEIGHT = .05f;
    private static final String BLANK_PREVIEW = "/images/generatedMapIcon.png";
    public static boolean DEBUG = false;

    static {
        String version = MapGenerator.class.getPackage().getImplementationVersion();
        VERSION = version != null ? version : "snapshot";
    }

    //read from cli args
    private String pathToFolder = ".";
    private String mapName = "";
    private long seed;
    private Random random;
    private boolean tournamentStyle = false;
    private boolean blind = false;
    private boolean unexplored = false;
    private long generationTime;

    //read from key value arguments or map name
    private int spawnCount = 6;
    private float landDensity;
    private float plateauDensity;
    private float mountainDensity;
    private float rampDensity;
    private int mapSize = 512;
    private int numTeams = 2;
    private float reclaimDensity;
    private int mexCount;
    private int hydroCount;
    private Symmetry terrainSymmetry;
    private Biome biome;

    private SCMap map;
    private float waterHeight;
    private boolean optionsUsed = false;

    //masks used in generation
    private ConcurrentBinaryMask land;
    private ConcurrentBinaryMask mountains;
    private ConcurrentBinaryMask hills;
    private ConcurrentBinaryMask valleys;
    private ConcurrentBinaryMask plateaus;
    private ConcurrentBinaryMask ramps;
    private ConcurrentBinaryMask connections;
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
    private ConcurrentBinaryMask fieldDecal;
    private ConcurrentBinaryMask slopeDecal;
    private ConcurrentBinaryMask mountainDecal;
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
    private BinaryMask noBases;
    private BinaryMask noCivs;

    private SymmetrySettings symmetrySettings;
    private boolean hasCivilians;
    private boolean enemyCivilians;
    private boolean landPathed;
    private int mountainBrushSize = 64;
    private int plateauBrushSize = 32;
    private int smallFeatureBrushSize = 24;
    private boolean validArgs;
    private boolean generationComplete;
    private boolean styleSpecified;
    private MapStyle mapStyle;
    private MapParameters mapParameters;
    private int numToGen = 1;
    private String previewFolder;

    public static void main(String[] args) throws Exception {

        int count = 0;
        Locale.setDefault(Locale.ENGLISH);

        MapGenerator generator = new MapGenerator();

        while (count < generator.numToGen) {
            generator.seed = new Random().nextLong();
            generator.interpretArguments(args);
            if (!generator.validArgs) {
                return;
            }

            System.out.println(generator.mapName);
            generator.generate();
            if (generator.map == null) {
                System.out.println("Map Generation Failed see stack trace for details");
                return;
            }
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
            System.out.println("Style: " + generator.mapStyle);
            System.out.println("Size: " + generator.mapSize);
            System.out.println("Done");
            if (generator.previewFolder != null) {
                SCMapExporter.exportPreview(Paths.get(generator.previewFolder), generator.map);
            }
            count++;
        }
    }

    public static Symmetry getValidSymmetry(int spawnCount, int numTeams, Random random) {
        List<Symmetry> terrainSymmetries;
        switch (spawnCount) {
            case 2:
                terrainSymmetries = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.POINT4, Symmetry.POINT6,
                        Symmetry.POINT8, Symmetry.QUAD, Symmetry.DIAG));
                break;
            case 4:
                terrainSymmetries = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.POINT4, Symmetry.POINT6,
                        Symmetry.POINT8, Symmetry.QUAD, Symmetry.DIAG, Symmetry.XZ, Symmetry.ZX));
                break;
            default:
                terrainSymmetries = new ArrayList<>(Arrays.asList(Symmetry.values()));
                break;
        }
        terrainSymmetries.remove(Symmetry.X);
        terrainSymmetries.remove(Symmetry.Z);
        if (numTeams != 0) {
            terrainSymmetries.remove(Symmetry.NONE);
            terrainSymmetries.removeIf(symmetry -> symmetry.getNumSymPoints() % numTeams != 0 || symmetry.getNumSymPoints() > spawnCount * 4);
        } else {
            terrainSymmetries.clear();
            terrainSymmetries.add(Symmetry.NONE);
        }
        if (random.nextFloat() < .75f) {
            terrainSymmetries.removeIf(symmetry -> !symmetry.isPerfectSymmetry());
        }
        return terrainSymmetries.get(random.nextInt(terrainSymmetries.size()));
    }

    public static int getMexCount(float mexDensity, int spawnCount, int mapSize) {
        int mexCount;
        float mexMultiplier = 1f;
        switch (spawnCount) {
            case 2:
                mexCount = (int) (10 + 20 * mexDensity);
                break;
            case 4:
                mexCount = (int) (9 + 8 * mexDensity);
                break;
            case 6:
            case 8:
                mexCount = (int) (8 + 5 * mexDensity);
                break;
            case 10:
                mexCount = (int) (8 + 3 * mexDensity);
                break;
            case 12:
                mexCount = (int) (6 + 4 * mexDensity);
                break;
            case 14:
                mexCount = (int) (6 + 3 * mexDensity);
                break;
            case 16:
                mexCount = (int) (6 + 2 * mexDensity);
                break;
            default:
                mexCount = (int) (8 + 8 * mexDensity);
                break;
        }
        if (mapSize < 512) {
            mexMultiplier = .75f;
        } else if (mapSize > 512) {
            switch (spawnCount) {
                case 2:
                case 4:
                case 6:
                    mexMultiplier = 1.5f;
                    break;
                case 8:
                case 10:
                    mexMultiplier = 1.35f;
                    break;
                default:
                    mexMultiplier = 1.25f;
                    break;
            }
        }
        mexCount *= mexMultiplier;
        return mexCount;
    }

    private void parseMapName() throws Exception {
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

    public static SymmetrySettings initSymmetrySettings(Symmetry terrainSymmetry, int spawnCount, int numTeams, Random random) {
        Symmetry spawnSymmetry;
        Symmetry teamSymmetry;
        List<Symmetry> spawns;
        List<Symmetry> teams;
        switch (terrainSymmetry) {
            case POINT2:
            case POINT3:
            case POINT4:
            case POINT5:
            case POINT6:
            case POINT7:
            case POINT8:
            case POINT9:
            case POINT10:
            case POINT11:
            case POINT12:
            case POINT13:
            case POINT14:
            case POINT15:
            case POINT16:
                spawns = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.POINT3, Symmetry.POINT4, Symmetry.POINT5,
                        Symmetry.POINT6, Symmetry.POINT7, Symmetry.POINT8, Symmetry.POINT9, Symmetry.POINT10, Symmetry.POINT11,
                        Symmetry.POINT12, Symmetry.POINT13, Symmetry.POINT14, Symmetry.POINT15, Symmetry.POINT16));
                teams = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.POINT3, Symmetry.POINT4, Symmetry.POINT5,
                        Symmetry.POINT6, Symmetry.POINT7, Symmetry.POINT8, Symmetry.POINT9, Symmetry.POINT10, Symmetry.POINT11,
                        Symmetry.POINT12, Symmetry.POINT13, Symmetry.POINT14, Symmetry.POINT15, Symmetry.POINT16,
                        Symmetry.XZ, Symmetry.ZX, Symmetry.QUAD, Symmetry.DIAG));
                break;
            case QUAD:
                spawns = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.QUAD));
                teams = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.QUAD));
                break;
            case DIAG:
                spawns = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.DIAG));
                teams = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.XZ, Symmetry.ZX, Symmetry.DIAG));
                break;
            default:
                spawns = new ArrayList<>(Collections.singletonList(terrainSymmetry));
                teams = new ArrayList<>(Collections.singletonList(terrainSymmetry));
                break;
        }
        if (numTeams != 0) {
            spawns.removeIf(symmetry -> spawnCount % symmetry.getNumSymPoints() != 0 || numTeams % symmetry.getNumSymPoints() != 0);
            teams.removeIf(symmetry -> spawnCount % symmetry.getNumSymPoints() != 0 || numTeams % symmetry.getNumSymPoints() != 0);
        }
        spawnSymmetry = spawns.get(random.nextInt(spawns.size()));
        teamSymmetry = teams.get(random.nextInt(teams.size()));
        return new SymmetrySettings(terrainSymmetry, teamSymmetry, spawnSymmetry);
    }

    public void interpretArguments(String[] args) throws Exception {
        styleSpecified = false;
        validArgs = true;
        generationComplete = true;
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
                    System.out.println("Seed not numeric using default seed or map name");
                }
                if (!VERSION.equals(args[2])) {
                    System.out.println("This generator only supports version " + VERSION);
                    validArgs = false;
                }
                if (args.length >= 4) {
                    mapName = args[3];
                    parseMapName();
                } else {
                    randomizeOptions();
                }
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                System.out.println("Usage: generator [targetFolder] [seed] [expectedVersion] (mapName)");
            }
        }
        if (!validArgs) {
            return;
        }
        symmetrySettings = initSymmetrySettings(terrainSymmetry, spawnCount, numTeams, random);
        if (!styleSpecified || mapStyle == null) {
            mapParameters = new MapParameters(spawnCount, landDensity, plateauDensity, mountainDensity, rampDensity,
                    reclaimDensity, mapSize, numTeams, mexCount, hydroCount, unexplored, symmetrySettings, biome);
            List<MapStyle> possibleStyles = new ArrayList<>(Arrays.asList(MapStyle.values()));
            possibleStyles.removeIf(style -> !style.matches(mapParameters));
            List<Float> weights = possibleStyles.stream().map(MapStyle::getWeight).collect(Collectors.toList());
            List<Float> cumulativeWeights = new ArrayList<>();
            float sum = 0;
            for (float weight : weights) {
                sum += weight;
                cumulativeWeights.add(sum);
            }
            float value = random.nextFloat() * cumulativeWeights.get(cumulativeWeights.size() - 1);
            mapStyle = cumulativeWeights.stream().filter(weight -> value <= weight)
                    .reduce((first, second) -> first)
                    .map(weight -> possibleStyles.get(cumulativeWeights.indexOf(weight)))
                    .orElse(MapStyle.DEFAULT);
        } else {
            mapParameters = mapStyle.initParameters(random, spawnCount, mapSize, numTeams, biome);
        }
        generateMapName();
    }

    private void interpretArguments(Map<String, String> arguments) throws Exception {
        if (arguments.containsKey("help")) {
            System.out.println("map-gen usage:\n" +
                    "--help                 produce help message\n" +
                    "--styles                 list styles\n" +
                    "--biomes                 list biomes\n" +
                    "--folder-path arg      optional, set the target folder for the generated map\n" +
                    "--seed arg             optional, set the seed for the generated map\n" +
                    "--map-name arg         optional, set the map name for the generated map\n" +
                    "--style arg            optional, set the map style for the generated map\n" +
                    "--spawn-count arg      optional, set the spawn count for the generated map\n" +
                    "--num-teams arg        optional, set the number of teams for the generated map (0 is no teams asymmetric)\n" +
                    "--land-density arg     optional, set the land density for the generated map\n" +
                    "--plateau-density arg  optional, set the plateau density for the generated map\n" +
                    "--mountain-density arg optional, set the mountain density for the generated map\n" +
                    "--ramp-density arg     optional, set the ramp density for the generated map\n" +
                    "--reclaim-density arg  optional, set the reclaim density for the generated map\n" +
                    "--mex-density arg      optional, set the mex density for the generated map\n" +
                    "--mex-count arg        optional, set the mex count per player for the generated map\n" +
                    "--map-size arg		    optional, set the map size (5km = 256, 10km = 512, 20km = 1024)\n" +
                    "--biome arg		    optional, set the biome\n" +
                    "--tournament-style     optional, set map to tournament style which will remove the preview.png and add time of original generation to map\n" +
                    "--blind                optional, set map to blind style which will apply tournament style and remove in game lobby preview\n" +
                    "--unexplored           optional, set map to unexplored style which will apply tournament and blind style and add unexplored fog of war\n" +
                    "--debug                optional, turn on debugging options\n" +
                    "--no-hash              optional, turn off pipeline hashing of masks\n" +
                    "--num-to-gen           optional, number of maps to generate\n" +
                    "--preview-path         optional, path to dump previews to\n");
            validArgs = false;
            return;
        }

        if (arguments.containsKey("styles")) {
            System.out.println("Valid Styles:\n" + Arrays.stream(MapStyle.values()).map(MapStyle::toString).collect(Collectors.joining("\n")));
            validArgs = false;
            return;
        }

        if (arguments.containsKey("symmetries")) {
            System.out.println("Valid Symmetries:\n" + Arrays.stream(Symmetry.values()).map(Symmetry::toString).collect(Collectors.joining("\n")));
            validArgs = false;
            return;
        }

        if (arguments.containsKey("biomes")) {
            System.out.println("Valid Biomes:\n" + String.join("\n", Biomes.BIOMES_LIST));
            validArgs = false;
            return;
        }

        if (arguments.containsKey("no-hash")) {
            Pipeline.HASH_MASK = false;
        }

        if (arguments.containsKey("debug")) {
            DEBUG = true;
        }

        if (arguments.containsKey("num-to-gen")) {
            numToGen = Integer.parseInt(arguments.get("num-to-gen"));
        }

        if (arguments.containsKey("folder-path")) {
            pathToFolder = arguments.get("folder-path");
        }

        if (arguments.containsKey("preview-path")) {
            previewFolder = arguments.get("preview-path");
        }

        if (arguments.containsKey("map-name") && arguments.get("map-name") != null) {
            mapName = arguments.get("map-name");
            parseMapName();
            return;
        }

        if (arguments.containsKey("style") && arguments.get("style") != null) {
            mapStyle = MapStyle.valueOf(arguments.get("style").toUpperCase(Locale.ROOT));
            styleSpecified = true;
        }

        tournamentStyle = arguments.containsKey("tournament-style") || arguments.containsKey("blind") || arguments.containsKey("unexplored");
        blind = arguments.containsKey("blind") || arguments.containsKey("unexplored");
        unexplored = arguments.containsKey("unexplored");

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

        if (arguments.containsKey("num-teams") && arguments.get("num-teams") != null) {
            numTeams = Integer.parseInt(arguments.get("num-teams"));
            if (numTeams != 2) {
                optionsUsed = true;
            }
        }

        randomizeOptions();

        if (!tournamentStyle && !styleSpecified) {
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

            if (arguments.containsKey("mex-density") && arguments.get("mex-density") != null) {
                float mexDensity = Float.parseFloat(arguments.get("mex-density"));
                mexCount = getMexCount(mexDensity, spawnCount, mapSize);
                optionsUsed = true;
            }

            if (arguments.containsKey("mex-count") && arguments.get("mex-count") != null) {
                mexCount = Integer.parseInt(arguments.get("mex-count"));
                optionsUsed = true;
            }

            if (arguments.containsKey("symmetry") && arguments.get("symmetry") != null) {
                terrainSymmetry = Symmetry.valueOf(arguments.get("symmetry").toUpperCase(Locale.ROOT));
                optionsUsed = true;
            }

            if (arguments.containsKey("biome") && arguments.get("biome") != null) {
                biome = Biomes.loadBiome(arguments.get("biome"));
                optionsUsed = true;
            }
        }
    }

    private void randomizeOptions() throws Exception {
        if (numTeams != 0 && spawnCount % numTeams != 0) {
            throw new IllegalArgumentException("spawnCount is not a multiple of number of teams");
        }
        random = new Random(new Random(seed).nextLong() ^ new Random(generationTime).nextLong());

        landDensity = StrictMath.round(RandomUtils.averageRandomFloat(random, 2) * 127) / 127f;
        plateauDensity = StrictMath.round(RandomUtils.averageRandomFloat(random, 2) * 127) / 127f;
        mountainDensity = StrictMath.round(RandomUtils.averageRandomFloat(random, 2) * 127) / 127f;
        rampDensity = StrictMath.round(RandomUtils.averageRandomFloat(random, 2) * 127) / 127f;
        reclaimDensity = StrictMath.round(RandomUtils.averageRandomFloat(random, 2) * 127) / 127f;
        mexCount = getMexCount(RandomUtils.averageRandomFloat(random, 2), spawnCount, mapSize);
        hydroCount = spawnCount >= 4 ? spawnCount + random.nextInt(spawnCount / 4) * 2 : (mapSize <= 512 ? spawnCount : spawnCount * (random.nextInt(3) + 1));
        terrainSymmetry = getValidSymmetry(spawnCount, numTeams, random);
        biome = Biomes.loadBiome(Biomes.BIOMES_LIST.get(random.nextInt(Biomes.BIOMES_LIST.size())));
    }

    private void parseOptions(byte[] optionBytes) throws Exception {
        if (optionBytes.length > 0) {
            if (optionBytes[0] <= 16) {
                spawnCount = optionBytes[0];
            }
        }
        if (optionBytes.length > 1) {
            mapSize = (int) optionBytes[1] * 64;
        }
        if (optionBytes.length > 2) {
            numTeams = optionBytes[2];
        }

        randomizeOptions();

        if (optionBytes.length > 3) {
            biome = Biomes.loadBiome(Biomes.BIOMES_LIST.get(optionBytes[3]));
        }

        if (optionBytes.length == 12) {
            landDensity = optionBytes[4] / 127f;
            plateauDensity = optionBytes[5] / 127f;
            mountainDensity = optionBytes[6] / 127f;
            rampDensity = optionBytes[7] / 127f;
            reclaimDensity = optionBytes[8] / 127f;
            mexCount = optionBytes[9];
            hydroCount = optionBytes[10];
            terrainSymmetry = Symmetry.values()[optionBytes[11]];
        } else if (optionBytes.length == 5) {
            mapStyle = MapStyle.values()[optionBytes[4]];
            styleSpecified = true;
        }
    }

    private void parseParameters(byte[] parameterBytes) {
        BitSet parameters = BitSet.valueOf(parameterBytes);
        tournamentStyle = parameters.get(0);
        blind = parameters.get(1);
        unexplored = parameters.get(2);
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
                    (byte) numTeams,
                    (byte) Biomes.BIOMES_LIST.indexOf(biome.getName()),
                    (byte) StrictMath.round(landDensity * 127f),
                    (byte) StrictMath.round(plateauDensity * 127f),
                    (byte) StrictMath.round(mountainDensity * 127f),
                    (byte) StrictMath.round(rampDensity * 127f),
                    (byte) StrictMath.round(reclaimDensity * 127f),
                    (byte) mexCount,
                    (byte) hydroCount,
                    (byte) terrainSymmetry.ordinal()};
        } else if (styleSpecified) {
            optionArray = new byte[]{(byte) spawnCount,
                    (byte) (mapSize / 64),
                    (byte) numTeams,
                    (byte) Biomes.BIOMES_LIST.indexOf(biome.getName()),
                    (byte) mapStyle.ordinal()};
        } else {
            optionArray = new byte[]{(byte) spawnCount,
                    (byte) (mapSize / 64)};
        }
        BitSet parameters = new BitSet();
        parameters.set(0, tournamentStyle);
        parameters.set(1, blind);
        parameters.set(2, unexplored);
        String optionString = NAME_ENCODER.encode(optionArray) + "_" + NAME_ENCODER.encode(parameters.toByteArray());
        if (tournamentStyle) {
            String timeString = NAME_ENCODER.encode(ByteBuffer.allocate(8).putLong(generationTime).array());
            optionString += "_" + timeString;
        }
        mapName = String.format(mapNameFormat, VERSION, seedString, optionString).toLowerCase();
    }

    public void save() {
        try {
            Path folderPath = Paths.get(pathToFolder);

            FileUtils.deleteRecursiveIfExists(folderPath.resolve(mapName));

            long startTime = System.currentTimeMillis();
            MapExporter.exportMap(folderPath, map, !tournamentStyle);
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

    public SCMap generate() throws Exception {
        long startTime = System.currentTimeMillis();
        long sTime = System.currentTimeMillis();

        if (DEBUG) {
            System.out.printf("Style selection done: %d ms\n", System.currentTimeMillis() - sTime);
        }

        map = mapStyle.generate(mapParameters, random);

        sTime = System.currentTimeMillis();
        map.setGeneratePreview(!blind);
        map.setUnexplored(unexplored);
        if (unexplored) {
            map.setCartographicContourInterval(100);
            map.setCartographicDeepWaterColor(1);
            map.setCartographicMapContourColor(1);
            map.setCartographicMapShoreColor(1);
            map.setCartographicMapLandStartColor(1);
            map.setCartographicMapLandEndColor(1);
        }
        if (!blind) {
            PreviewGenerator.generatePreview(map);
        } else {
            BufferedImage blindPreview = readImage(BLANK_PREVIEW);
            map.getPreview().setData(blindPreview.getData());
        }
        StringBuilder descriptionBuilder = new StringBuilder();
        if (tournamentStyle) {
            descriptionBuilder.append(String.format("Map originally generated at %s UTC. ",
                    DateTimeFormatter.ofPattern("HH:mm:ss dd MMM uuuu")
                            .format(Instant.ofEpochSecond(generationTime).atZone(ZoneOffset.UTC))));
        }
        if (unexplored) {
            descriptionBuilder.append("Use with the Unexplored Maps Mod for best experience");
        }
        map.setDescription(descriptionBuilder.toString());
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, generatePreview\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(MapGenerator.class));
        }
        ScriptGenerator.generateScript(map);

        System.out.printf("Map generation done: %d ms\n", System.currentTimeMillis() - startTime);

        map.addBlank(new Marker(mapName, new Vector2f(0, 0)));
        map.addDecalGroup(new DecalGroup(mapName, new int[0]));

        if (!generationComplete) {
            return null;
        }

        map.setName(mapName);
        map.setFolderName(mapName);
        map.setFilePrefix(mapName);

        return map;
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
                    "\nSpawn Symmetry: " + symmetrySettings.getSpawnSymmetry() +
                    "\nStyle: " + mapStyle.toString();
            out.write(summaryString.getBytes());
            out.flush();
            out.close();
        }
    }
}
