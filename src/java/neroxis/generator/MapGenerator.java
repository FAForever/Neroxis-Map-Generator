package neroxis.generator;

import com.google.common.io.BaseEncoding;
import lombok.Getter;
import lombok.Setter;
import neroxis.biomes.Biome;
import neroxis.biomes.Biomes;
import neroxis.exporter.MapExporter;
import neroxis.exporter.SCMapExporter;
import neroxis.generator.style.*;
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
    private static final String BLANK_PREVIEW = "/images/generatedMapIcon.png";
    public static boolean DEBUG = false;

    static {
        String version = MapGenerator.class.getPackage().getImplementationVersion();
        VERSION = version != null ? version : "snapshot";
    }

    private final int numBins = 127;
    private final List<StyleGenerator> mapStyles = Collections.unmodifiableList(Arrays.asList(new BigIslandsStyleGenerator(), new CenterLakeStyleGenerator(),
            new DefaultStyleGenerator(), new DropPlateauStyleGenerator(), new LandBridgeStyleGenerator(), new LittleMountainStyleGenerator(),
            new MountainRangeStyleGenerator(), new OneIslandStyleGenerator(), new SmallIslandsStyleGenerator(), new ValleyStyleGenerator()));

    //read from cli args
    private String pathToFolder = ".";
    private String mapName;
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
    private float mexDensity;
    private int hydroCount;
    private Symmetry terrainSymmetry;
    private Biome biome;

    private SCMap map;
    private boolean optionsUsed = false;

    private SymmetrySettings symmetrySettings;
    private boolean validArgs;
    private boolean generationComplete;
    private boolean styleSpecified;
    private StyleGenerator mapStyle;
    private MapParameters mapParameters;
    private int numToGen = 1;
    private String previewFolder;

    public static void main(String[] args) throws Exception {

        int count = 0;
        Locale.setDefault(Locale.ROOT);

        MapGenerator generator = new MapGenerator();

        while (count < generator.numToGen) {
            generator.mapName = null;
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
            System.out.println(generator.mapParameters.toString());
            System.out.println("Style: " + generator.mapStyle.getName());
            System.out.println("Done");
            if (generator.previewFolder != null) {
                SCMapExporter.exportPreview(Paths.get(generator.previewFolder), generator.map);
            }
            count++;
        }
    }

    public void setValidTerrainSymmetry() {
        List<Symmetry> terrainSymmetries;
        switch (spawnCount) {
            case 2:
            case 4:
                terrainSymmetries = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.POINT4, Symmetry.POINT6,
                        Symmetry.POINT8, Symmetry.QUAD, Symmetry.DIAG));
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
        if (numTeams == 2 && random.nextFloat() < .75f) {
            terrainSymmetries.removeIf(symmetry -> !symmetry.isPerfectSymmetry());
        }
        terrainSymmetry = terrainSymmetries.get(random.nextInt(terrainSymmetries.size()));
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

    public void setSymmetrySettings() {
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
                        Symmetry.XZ, Symmetry.ZX, Symmetry.X, Symmetry.Z, Symmetry.QUAD, Symmetry.DIAG));
                break;
            case QUAD:
                spawns = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.QUAD));
                teams = new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.X, Symmetry.Z, Symmetry.QUAD));
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
            spawns.removeIf(symmetry -> spawnCount % symmetry.getNumSymPoints() != 0 || symmetry.getNumSymPoints() % numTeams != 0);
            teams.removeIf(symmetry -> spawnCount % symmetry.getNumSymPoints() != 0 || symmetry.getNumSymPoints() % numTeams != 0);
        }
        spawnSymmetry = spawns.get(random.nextInt(spawns.size()));
        teamSymmetry = teams.get(random.nextInt(teams.size()));
        symmetrySettings = new SymmetrySettings(terrainSymmetry, teamSymmetry, spawnSymmetry);
    }

    private void setMapStyle() {
        mapParameters = MapParameters.builder()
                .spawnCount(spawnCount)
                .landDensity(landDensity)
                .plateauDensity(plateauDensity)
                .mountainDensity(mountainDensity)
                .rampDensity(rampDensity)
                .reclaimDensity(reclaimDensity)
                .mexDensity(mexDensity)
                .mapSize(mapSize)
                .numTeams(numTeams)
                .hydroCount(spawnCount)
                .unexplored(unexplored)
                .symmetrySettings(symmetrySettings)
                .biome(biome)
                .build();
        mapStyle = RandomUtils.selectRandomMatchingGenerator(random, mapStyles, mapParameters, new DefaultStyleGenerator());
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
        setSymmetrySettings();
        if (!styleSpecified || mapStyle == null) {
            setMapStyle();
        } else {
            mapParameters = mapStyle.getParameterConstraints().initParameters(random, spawnCount, mapSize, numTeams, biome, symmetrySettings);
        }
        if (mapName == null) {
            generateMapName();
        }
    }

    private void interpretArguments(Map<String, String> arguments) throws Exception {
        if (arguments.containsKey("help")) {
            System.out.println("map-gen usage:\n" +
                    "--help                 produce help message\n" +
                    "--styles               list styles\n" +
                    "--biomes               list biomes\n" +
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
                    "--map-size arg         optional, set the map size (5km = 256, 10km = 512, 20km = 1024)\n" +
                    "--biome arg            optional, set the biome\n" +
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
            System.out.println("Valid Styles:\n" + mapStyles.stream().map(StyleGenerator::getName).collect(Collectors.joining("\n")));
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

        tournamentStyle = arguments.containsKey("tournament-style") || arguments.containsKey("blind") || arguments.containsKey("unexplored");
        blind = arguments.containsKey("blind") || arguments.containsKey("unexplored");
        unexplored = arguments.containsKey("unexplored");

        if (tournamentStyle) {
            generationTime = Instant.now().getEpochSecond();
        }

        if (arguments.containsKey("spawn-count") && arguments.get("spawn-count") != null) {
            spawnCount = Integer.parseInt(arguments.get("spawn-count"));
        }

        if (arguments.containsKey("map-size") && arguments.get("map-size") != null) {
            mapSize = Integer.parseInt(arguments.get("map-size"));
        }

        if (arguments.containsKey("num-teams") && arguments.get("num-teams") != null) {
            numTeams = Integer.parseInt(arguments.get("num-teams"));
        }

        if (arguments.containsKey("seed") && arguments.get("seed") != null) {
            seed = Long.parseLong(arguments.get("seed"));
        }

        randomizeOptions();

        if (!tournamentStyle) {

            if (arguments.containsKey("style") && arguments.get("style") != null) {
                mapStyle = mapStyles.stream().filter(style -> style.getName().equals(arguments.get("style").toUpperCase(Locale.ROOT)))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported Map Style"));
                styleSpecified = true;
            }

            if (!styleSpecified) {
                if (arguments.containsKey("land-density") && arguments.get("land-density") != null) {
                    landDensity = ParseUtils.discretePercentage(Float.parseFloat(arguments.get("land-density")), numBins);
                    optionsUsed = true;
                }

                if (arguments.containsKey("plateau-density") && arguments.get("plateau-density") != null) {
                    plateauDensity = ParseUtils.discretePercentage(Float.parseFloat(arguments.get("plateau-density")), numBins);
                    optionsUsed = true;
                }

                if (arguments.containsKey("mountain-density") && arguments.get("mountain-density") != null) {
                    mountainDensity = ParseUtils.discretePercentage(Float.parseFloat(arguments.get("mountain-density")), numBins);
                    optionsUsed = true;
                }

                if (arguments.containsKey("ramp-density") && arguments.get("ramp-density") != null) {
                    rampDensity = ParseUtils.discretePercentage(Float.parseFloat(arguments.get("ramp-density")), numBins);
                    optionsUsed = true;
                }

                if (arguments.containsKey("reclaim-density") && arguments.get("reclaim-density") != null) {
                    reclaimDensity = ParseUtils.discretePercentage(Float.parseFloat(arguments.get("reclaim-density")), numBins);
                    optionsUsed = true;
                }

                if (arguments.containsKey("mex-density") && arguments.get("mex-density") != null) {
                    mexDensity = ParseUtils.discretePercentage(Float.parseFloat(arguments.get("mex-density")), numBins);
                    optionsUsed = true;
                }

                if (arguments.containsKey("symmetry") && arguments.get("symmetry") != null) {
                    terrainSymmetry = Symmetry.valueOf(arguments.get("symmetry").toUpperCase());
                    optionsUsed = true;
                }

                if (arguments.containsKey("biome") && arguments.get("biome") != null) {
                    biome = Biomes.loadBiome(arguments.get("biome"));
                    optionsUsed = true;
                }
            }
        }
    }

    private void randomizeOptions() throws Exception {
        if (numTeams != 0 && spawnCount % numTeams != 0) {
            throw new IllegalArgumentException("spawnCount is not a multiple of number of teams");
        }
        random = new Random(new Random(seed).nextLong() ^ new Random(generationTime).nextLong());

        landDensity = ParseUtils.discretePercentage(RandomUtils.averageRandomFloat(random, 2), numBins);
        plateauDensity = ParseUtils.discretePercentage(RandomUtils.averageRandomFloat(random, 2), numBins);
        mountainDensity = ParseUtils.discretePercentage(RandomUtils.averageRandomFloat(random, 2), numBins);
        rampDensity = ParseUtils.discretePercentage(RandomUtils.averageRandomFloat(random, 2), numBins);
        reclaimDensity = ParseUtils.discretePercentage(RandomUtils.averageRandomFloat(random, 2), numBins);
        mexDensity = ParseUtils.discretePercentage(RandomUtils.averageRandomFloat(random, 2), numBins);
        hydroCount = spawnCount >= 4 ? spawnCount + random.nextInt(spawnCount / 4) * 2 : (mapSize <= 512 ? spawnCount : spawnCount * (random.nextInt(3) + 1));
        setValidTerrainSymmetry();
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

        if (optionBytes.length > 3) {
            biome = Biomes.loadBiome(Biomes.BIOMES_LIST.get(optionBytes[3]));
        }

        randomizeOptions();

        if (optionBytes.length == 12) {
            landDensity = ParseUtils.normalizeBin(optionBytes[4], numBins);
            plateauDensity = ParseUtils.normalizeBin(optionBytes[5], numBins);
            mountainDensity = ParseUtils.normalizeBin(optionBytes[6], numBins);
            rampDensity = ParseUtils.normalizeBin(optionBytes[7], numBins);
            reclaimDensity = ParseUtils.normalizeBin(optionBytes[8], numBins);
            mexDensity = ParseUtils.normalizeBin(optionBytes[9], numBins);
            hydroCount = optionBytes[10];
            terrainSymmetry = Symmetry.values()[optionBytes[11]];
        } else if (optionBytes.length == 5) {
            mapStyle = mapStyles.get(optionBytes[4]);
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
                    (byte) ParseUtils.binPercentage(landDensity, numBins),
                    (byte) ParseUtils.binPercentage(plateauDensity, numBins),
                    (byte) ParseUtils.binPercentage(mountainDensity, numBins),
                    (byte) ParseUtils.binPercentage(rampDensity, numBins),
                    (byte) ParseUtils.binPercentage(reclaimDensity, numBins),
                    (byte) ParseUtils.binPercentage(mexDensity, numBins),
                    (byte) hydroCount,
                    (byte) terrainSymmetry.ordinal()};
        } else if (styleSpecified) {
            int styleIndex = mapStyles.indexOf(mapStyles.stream().filter(styleGenerator -> mapStyle.getName().equals(styleGenerator.getName()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported Map Style")));
            optionArray = new byte[]{(byte) spawnCount,
                    (byte) (mapSize / 64),
                    (byte) numTeams,
                    (byte) Biomes.BIOMES_LIST.indexOf(biome.getName()),
                    (byte) styleIndex};
        } else {
            optionArray = new byte[]{(byte) spawnCount,
                    (byte) (mapSize / 64),
                    (byte) numTeams};
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

        map = mapStyle.generate(mapParameters, random.nextLong());

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
                    Util.getStackTraceLineInPackage("neroxis.generator"));
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
                    "\n" + mapParameters.toString() +
                    "\nStyle: " + mapStyle.toString();
            out.write(summaryString.getBytes());
            out.flush();
            out.close();
        }
    }
}
