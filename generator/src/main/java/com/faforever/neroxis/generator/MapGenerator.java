package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.cli.BiomeParameterConsumer;
import com.faforever.neroxis.cli.MapSizeParameterConsumer;
import com.faforever.neroxis.cli.WritableFolderParameterConsumer;
import com.faforever.neroxis.generator.cli.DensityParameterConsumer;
import com.faforever.neroxis.generator.style.BasicStyleGenerator;
import com.faforever.neroxis.generator.style.BigIslandsStyleGenerator;
import com.faforever.neroxis.generator.style.CenterLakeStyleGenerator;
import com.faforever.neroxis.generator.style.DropPlateauStyleGenerator;
import com.faforever.neroxis.generator.style.FloodedStyleGenerator;
import com.faforever.neroxis.generator.style.HighReclaimStyleGenerator;
import com.faforever.neroxis.generator.style.LandBridgeStyleGenerator;
import com.faforever.neroxis.generator.style.LittleMountainStyleGenerator;
import com.faforever.neroxis.generator.style.LowMexStyleGenerator;
import com.faforever.neroxis.generator.style.MountainRangeStyleGenerator;
import com.faforever.neroxis.generator.style.OneIslandStyleGenerator;
import com.faforever.neroxis.generator.style.SmallIslandsStyleGenerator;
import com.faforever.neroxis.generator.style.StyleGenerator;
import com.faforever.neroxis.generator.style.TestStyleGenerator;
import com.faforever.neroxis.generator.style.ValleyStyleGenerator;
import com.faforever.neroxis.map.DecalGroup;
import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.exporter.MapExporter;
import com.faforever.neroxis.map.exporter.SCMapExporter;
import com.faforever.neroxis.map.exporter.ScriptGenerator;
import com.faforever.neroxis.util.ArgumentParser;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.MathUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.SymmetrySelector;
import com.faforever.neroxis.util.vector.Vector2;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static picocli.CommandLine.ArgGroup;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Getter
@Setter
@Command(name = "generate", version = "tmp", mixinStandardHelpOptions = true, description = "Generates a map from scratch")
public strictfp class MapGenerator implements Callable<Integer> {

    private static final String VERSION;
    private static int NUM_TO_GEN = 1;
    public static final int NUM_BINS = 127;
    private static final List<StyleGenerator> MAP_STYLES = List.of(new BigIslandsStyleGenerator(), new CenterLakeStyleGenerator(), new BasicStyleGenerator(), new DropPlateauStyleGenerator(), new LandBridgeStyleGenerator(), new LittleMountainStyleGenerator(), new MountainRangeStyleGenerator(), new OneIslandStyleGenerator(), new SmallIslandsStyleGenerator(), new ValleyStyleGenerator(), new HighReclaimStyleGenerator(), new LowMexStyleGenerator(), new FloodedStyleGenerator(), new TestStyleGenerator());
    private static final List<StyleGenerator> PRODUCTION_STYLES = MAP_STYLES.stream().filter(styleGenerator -> !(styleGenerator instanceof TestStyleGenerator)).collect(Collectors.toList());

    static {
        String version = MapGenerator.class.getPackage().getImplementationVersion();
        VERSION = version != null ? version : "snapshot";
    }

    @Spec
    private CommandLine.Model.CommandSpec spec;

    //read from cli args
    @Option(names = {"--folder-path"}, defaultValue = ".", description = "Folder to save the map in", parameterConsumer = WritableFolderParameterConsumer.class)
    private Path folderPath;
    @Option(names = {"--preview-path"}, defaultValue = ".", description = "Folder to save the map previews to")
    private String previewFolder;
    @ArgGroup
    private GeneratorOptions options;

    @Getter
    @SuppressWarnings("unused")
    static strictfp class GeneratorOptions {
        @Option(names = {"--map-name"}, description = "Name of map to recreate. Must be of the form neroxis_map_generator_version_seed_options")
        private String mapName;

        @ArgGroup(heading = "Map parameters for generation", exclusive = false)
        MapParameters mapParameters;
    }

    @Getter
    @SuppressWarnings("unused")
    static strictfp class MapParameters {
        @Option(names = "--spawn-count", defaultValue = "6", description = "Spawn count for the generated map")
        private int spawnCount;
        @Option(names = "--map-size", defaultValue = "512", description = "Generated map size, can be specified in oGrids (e.g 512) or km (e.g 10km)", parameterConsumer = MapSizeParameterConsumer.class)
        private int mapSize;
        @Option(names = "--num-teams", defaultValue = "2", description = "Number of teams for the generated map (0 is no teams asymmetric)")
        private int numTeams;

        @ArgGroup(heading = "Options that control map generation")
        private MapSpecifications mapSpecifications;
    }

    @Getter
    @SuppressWarnings("unused")
    static strictfp class MapSpecifications {
        @ArgGroup(heading = "Options that provide fine grained control over map generation")
        private ParameterOptions parameterOptions;

        private StyleGenerator mapStyle;

        @Option(names = "--style", description = "Style for the generated map")
        public void setMapStyle(String style) {
            mapStyle = MAP_STYLES.stream()
                    .filter(mapStyle -> mapStyle.getName().equals(style.toUpperCase(Locale.ROOT)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported Map Style"));
        }

        @ArgGroup(heading = "Options that affect the visibility of the resulting generated map. Not allowed with greater specifications")
        VisibilityOptions visibilityOptions;
    }

    @Getter
    @SuppressWarnings("unused")
    static strictfp class ParameterOptions {
        @Option(names = "--seed", description = "Seed for the generated map")
        private long seed;
        @Option(names = "--land-density", description = "Land density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private float landDensity;
        @Option(names = "--plateau-density", description = "Plateau density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private float plateauDensity;
        @Option(names = "--mountain-density", description = "Mountain density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private float mountainDensity;
        @Option(names = "--ramp-density", description = "Ramp density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private float rampDensity;
        @Option(names = "--reclaim-density", description = "Reclaim density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private float reclaimDensity;
        @Option(names = "--mex-density", description = "Mex density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private float mexDensity;
        @Option(names = "--terrain-symmetry", description = "Base terrain symmetry for the map")
        private Symmetry terrainSymmetry;
        @Option(names = "--biome", description = "Texture biome for the generated map", parameterConsumer = BiomeParameterConsumer.class)
        private Biome biome;
    }

    @Getter
    @SuppressWarnings("unused")
    static strictfp class VisibilityOptions {
        @Option(names = "--tournament-style", description = "Remove the preview.png and add time of original generation to map")
        private boolean tournamentStyle;
        @Option(names = "--blind", description = "Remove the preview.png, add time of original generation to map, and remove in game lobby preview")
        private boolean blind;
        @Option(names = "--unexplored", description = "Remove the preview.png, add time of original generation to map, remove in game lobby preview, and add unexplored fog of war")
        private boolean unexplored;
    }

    private SCMap map;
    private StyleGenerator mapStyle;
    private String mapName;
    private long seed;
    private long generationTime;
    private Random random;
    private Symmetry terrainSymmetry;
    private GeneratorParameters generatorParameters;
    private GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder = GeneratorParameters.builder();

    public static void main(String[] args) throws Exception {

        int count = 0;
        Locale.setDefault(Locale.ROOT);

        while (count < MapGenerator.NUM_TO_GEN) {
            Pipeline.reset();
            MapGenerator generator = new MapGenerator();
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
            System.out.println(generator.mapStyle.generatorParameters.toString());
            System.out.println("Style: " + generator.mapStyle.getName());
            System.out.println(generator.mapStyle.generatorsToString());
            if (!generator.tournamentStyle && generator.previewFolder != null) {
                SCMapExporter.exportPreview(Paths.get(generator.previewFolder), generator.map);
            }
            count++;
        }
        Pipeline.shutdown();
    }

    private void setValidTerrainSymmetry() {
        GeneratorParameters tempGeneratorParameters = generatorParametersBuilder.build();
        List<Symmetry> terrainSymmetries = switch (tempGeneratorParameters.getSpawnCount()) {
            case 2, 4 -> new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.POINT4, Symmetry.POINT6,
                    Symmetry.POINT8, Symmetry.QUAD, Symmetry.DIAG));
            default -> new ArrayList<>(Arrays.asList(Symmetry.values()));
        };
        terrainSymmetries.remove(Symmetry.X);
        terrainSymmetries.remove(Symmetry.Z);
        int numTeams = tempGeneratorParameters.getNumTeams();
        if (numTeams > 1) {
            terrainSymmetries.remove(Symmetry.NONE);
            terrainSymmetries.removeIf(symmetry -> symmetry.getNumSymPoints() % numTeams != 0 || symmetry.getNumSymPoints() > spawnCount * 4);
        } else {
            terrainSymmetries.clear();
            terrainSymmetries.add(Symmetry.NONE);
        }
        if (numTeams == 2 && random.nextFloat() < .75f) {
            terrainSymmetries.removeIf(symmetry -> !symmetry.isPerfectSymmetry());
        }
        generatorParametersBuilder.terrainSymmetry(terrainSymmetries.get(random.nextInt(terrainSymmetries.size())));
    }

    private void parseMapName() throws Exception {
        if (!mapName.startsWith("neroxis_map_generator")) {
            throw new IllegalArgumentException("Map name is not a generated map");
        }

        String[] nameArgs = mapName.split("_");
        if (nameArgs.length < 4) {
            throw new RuntimeException("Version not specified");
        }

        String version = nameArgs[3];
        if (!VERSION.equals(version)) {
            throw new RuntimeException("Wrong generator version: " + version);
        }

        if (nameArgs.length < 5) {
            throw new RuntimeException("Seed not specified");
        }

        String seedString = nameArgs[4];
        try {
            seed = Long.parseLong(seedString);
        } catch (NumberFormatException nfe) {
            byte[] seedBytes = GeneratedMapNameEncoder.decode(seedString);
            ByteBuffer seedWrapper = ByteBuffer.wrap(seedBytes);
            seed = seedWrapper.getLong();
        }

        byte[] optionBytes = new byte[0];

        if (nameArgs.length >= 6) {
            String optionString = nameArgs[5];
            optionBytes = GeneratedMapNameEncoder.decode(optionString);
        }

        if (nameArgs.length >= 7) {
            String parametersString = nameArgs[6];
            byte[] parameterBytes = GeneratedMapNameEncoder.decode(parametersString);
            parseParameters(parameterBytes);
        }

        if (nameArgs.length >= 8) {
            String timeString = nameArgs[7];
            generationTime = ByteBuffer.wrap(GeneratedMapNameEncoder.decode(timeString)).getLong();
        }

        parseOptions(optionBytes);
    }

    private void parseOptions(byte[] optionBytes) throws Exception {
        if (optionBytes.length > 0) {
            if (optionBytes[0] <= 16) {
                generatorParametersBuilder.spawnCount(optionBytes[0]);
            }
        }
        if (optionBytes.length > 1) {
            generatorParametersBuilder.mapSize(optionBytes[1] * 64);
        }

        if (optionBytes.length > 2) {
            generatorParametersBuilder.numTeams(optionBytes[2]);
        }

        randomizeOptions();

        if (optionBytes.length == 11) {
            generatorParametersBuilder.biome(Biomes.loadBiome(Biomes.BIOMES_LIST.get(optionBytes[3])));
            generatorParametersBuilder.landDensity(MathUtil.normalizeBin(optionBytes[4], NUM_BINS));
            generatorParametersBuilder.plateauDensity(MathUtil.normalizeBin(optionBytes[5], NUM_BINS));
            generatorParametersBuilder.mountainDensity(MathUtil.normalizeBin(optionBytes[6], NUM_BINS));
            generatorParametersBuilder.rampDensity(MathUtil.normalizeBin(optionBytes[7], NUM_BINS));
            generatorParametersBuilder.reclaimDensity(MathUtil.normalizeBin(optionBytes[8], NUM_BINS));
            generatorParametersBuilder.mexDensity(MathUtil.normalizeBin(optionBytes[9], NUM_BINS));
            generatorParametersBuilder.terrainSymmetry(Symmetry.values()[optionBytes[10]]);
        } else if (optionBytes.length == 4) {
            mapStyle = MAP_STYLES.get(optionBytes[3]);
        }
    }

    private void parseParameters(byte[] parameterBytes) {
        BitSet parameters = BitSet.valueOf(parameterBytes);
        generatorParametersBuilder.tournamentStyle(parameters.get(0));
        generatorParametersBuilder.blind(parameters.get(1));
        generatorParametersBuilder.unexplored(parameters.get(2));
    }

    private void setMapStyle() {
        generatorParameters = generatorParametersBuilder.build();
        mapStyle = StyleGenerator.selectRandomMatchingGenerator(random, PRODUCTION_STYLES, generatorParameters, new BasicStyleGenerator());
    }

    public void interpretArguments(String... args) throws Exception {
        interpretArguments(ArgumentParser.parse(args));
    }

    public void interpretArguments(Map<String, String> arguments) throws Exception {
        if (arguments.containsKey("help")) {
            System.out.println("""
                    map-gen usage:
                    --help                 produce help message
                    --styles               list styles
                    --weights              list styles and weights based on the given parameters
                    --biomes               list biomes
                    --folder-path arg      optional, set the target folder for the generated map
                    --seed arg             optional, set the seed for the generated map
                    --map-name arg         optional, set the map name for the generated map
                    --style arg            optional, set the map style for the generated map
                    --spawn-count arg      optional, set the spawn count for the generated map
                    --num-teams arg        optional, set the number of teams for the generated map (0 is no teams asymmetric)
                    --land-density arg     optional, set the land density for the generated map
                    --plateau-density arg  optional, set the plateau density for the generated map
                    --mountain-density arg optional, set the mountain density for the generated map
                    --ramp-density arg     optional, set the ramp density for the generated map
                    --reclaim-density arg  optional, set the reclaim density for the generated map
                    --mex-density arg      optional, set the mex density for the generated map
                    --mex-count arg        optional, set the mex count per player for the generated map
                    --map-size arg         optional, set the map size (5km = 256, 10km = 512, 20km = 1024)
                    --biome arg            optional, set the biome
                    --tournament-style     optional, set map to tournament style which will remove the preview.png and add time of original generation to map
                    --blind                optional, set map to blind style which will apply tournament style and remove in game lobby preview
                    --unexplored           optional, set map to unexplored style which will apply tournament and blind style and add unexplored fog of war
                    --debug                optional, turn on debugging options
                    --num-to-gen           optional, number of maps to generate
                    --preview-path         optional, path to dump previews to
                    """);
            validArgs = false;
            return;
        }

        if (arguments.containsKey("styles")) {
            System.out.println("Valid Styles:\n" + PRODUCTION_STYLES.stream().map(StyleGenerator::getName).collect(Collectors.joining("\n")));
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

        styleSpecified = false;
        validArgs = true;
        seed = new Random().nextLong();

        if (arguments.containsKey("debug")) {
            DebugUtil.DEBUG = true;
            Pipeline.HASH_MASK = true;
        }

        if (arguments.containsKey("num-to-gen")) {
            NUM_TO_GEN = Integer.parseInt(arguments.get("num-to-gen"));
        }

        if (arguments.containsKey("folder-path")) {
            pathToFolder = arguments.get("folder-path");
        }

        if (arguments.containsKey("preview-path")) {
            previewFolder = arguments.get("preview-path");
            Files.createDirectories(Paths.get(previewFolder));
        }

        if (arguments.containsKey("map-name") && arguments.get("map-name") != null) {
            mapName = arguments.get("map-name");
            parseMapName();
            return;
        }

        unexplored = arguments.containsKey("unexplored");
        blind = arguments.containsKey("blind") || unexplored;
        tournamentStyle = arguments.containsKey("tournament-style") || blind;

        if (arguments.containsKey("spawn-count") && arguments.get("spawn-count") != null) {
            spawnCount = Integer.parseInt(arguments.get("spawn-count"));
        }

        if (arguments.containsKey("num-teams") && arguments.get("num-teams") != null) {
            numTeams = Integer.parseInt(arguments.get("num-teams"));
        }

        if (arguments.containsKey("map-size") && arguments.get("map-size") != null) {
            mapSize = StrictMath.round(Integer.parseInt(arguments.get("map-size")) / 64f) * 64;
        }

        if (arguments.containsKey("seed") && arguments.get("seed") != null) {
            seed = Long.parseLong(arguments.get("seed"));
        }

        if (tournamentStyle) {
            generationTime = Instant.now().getEpochSecond();
        }

        if (numTeams != 0 && spawnCount % numTeams != 0) {
            throw new IllegalArgumentException("spawnCount is not a multiple of number of teams");
        }

        if (arguments.containsKey("weights")) {
            List<StyleGenerator> generators = PRODUCTION_STYLES.stream()
                    .filter(generator -> {
                        ParameterConstraints constraints = generator.getParameterConstraints();
                        return constraints.getMapSizeRange().contains(mapSize)
                                && constraints.getNumTeamsRange().contains(numTeams)
                                && constraints.getSpawnCountRange().contains(spawnCount);
                    }).collect(Collectors.toList());
            float totalWeights = (float) generators.stream().mapToDouble(StyleGenerator::getWeight).sum();
            String styleWeights = generators.stream().map(generator -> String.format("%s: %.2f", generator.getName(), generator.getWeight() / totalWeights))
                    .collect(Collectors.joining(", "));
            System.out.println("Style Weights: " + styleWeights);
            validArgs = false;
            return;
        }

        randomizeOptions();

        if (!tournamentStyle) {

            if (arguments.containsKey("style") && arguments.get("style") != null) {
                mapStyle = MAP_STYLES.stream().filter(style -> style.getName().equals(arguments.get("style").toUpperCase(Locale.ROOT)))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported Map Style"));
                styleSpecified = true;
            }

            if (!styleSpecified) {
                if (arguments.containsKey("land-density") && arguments.get("land-density") != null) {
                    landDensity = MathUtil.discretePercentage(Float.parseFloat(arguments.get("land-density")), NUM_BINS);
                    optionsUsed = true;
                }

                if (arguments.containsKey("plateau-density") && arguments.get("plateau-density") != null) {
                    plateauDensity = MathUtil.discretePercentage(Float.parseFloat(arguments.get("plateau-density")), NUM_BINS);
                    optionsUsed = true;
                }

                if (arguments.containsKey("mountain-density") && arguments.get("mountain-density") != null) {
                    mountainDensity = MathUtil.discretePercentage(Float.parseFloat(arguments.get("mountain-density")), NUM_BINS);
                    optionsUsed = true;
                }

                if (arguments.containsKey("ramp-density") && arguments.get("ramp-density") != null) {
                    rampDensity = MathUtil.discretePercentage(Float.parseFloat(arguments.get("ramp-density")), NUM_BINS);
                    optionsUsed = true;
                }

                if (arguments.containsKey("reclaim-density") && arguments.get("reclaim-density") != null) {
                    reclaimDensity = MathUtil.discretePercentage(Float.parseFloat(arguments.get("reclaim-density")), NUM_BINS);
                    optionsUsed = true;
                }

                if (arguments.containsKey("mex-density") && arguments.get("mex-density") != null) {
                    mexDensity = MathUtil.discretePercentage(Float.parseFloat(arguments.get("mex-density")), NUM_BINS);
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

        symmetrySettings = SymmetrySelector.getSymmetrySettingsFromTerrainSymmetry(random, terrainSymmetry, numTeams);

        if (optionsUsed) {
            setMapStyle();
        } else if (styleSpecified) {
            mapParameters = mapStyle.getParameterConstraints().initParameters(random, spawnCount, mapSize, numTeams, tournamentStyle, blind, unexplored, symmetrySettings);
        } else {
            mapStyle = StyleGenerator.selectRandomMatchingGenerator(random, PRODUCTION_STYLES, spawnCount, mapSize, numTeams, new BasicStyleGenerator());
            mapParameters = mapStyle.getParameterConstraints().initParameters(random, spawnCount, mapSize, numTeams, tournamentStyle, blind, unexplored, symmetrySettings);
        }

        if (mapName == null) {
            generateMapName();
        }

        folderPath = Paths.get(pathToFolder);
        FileUtil.deleteRecursiveIfExists(folderPath.resolve(mapName));
    }

    private void randomizeOptions() throws Exception {
        random = new Random(new Random(seed).nextLong() ^ new Random(generationTime).nextLong());

        generatorParametersBuilder.landDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.plateauDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.mountainDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.rampDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.reclaimDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.mexDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.biome(Biomes.loadBiome(Biomes.BIOMES_LIST.get(random.nextInt(Biomes.BIOMES_LIST.size()))));
        setValidTerrainSymmetry();
    }

    private void generateMapName() {
        String mapNameFormat = "neroxis_map_generator_%s_%s_%s";
        ByteBuffer seedBuffer = ByteBuffer.allocate(8);
        seedBuffer.putLong(seed);
        String seedString = GeneratedMapNameEncoder.encode(seedBuffer.array());
        byte[] optionArray;
        if (optionsUsed) {
            optionArray = new byte[]{(byte) generatorParameters.getSpawnCount(),
                    (byte) (generatorParameters.getMapSize() / 64),
                    (byte) generatorParameters.getNumTeams(),
                    (byte) Biomes.BIOMES_LIST.indexOf(generatorParameters.getBiome().getName()),
                    (byte) MathUtil.binPercentage(generatorParameters.getLandDensity(), NUM_BINS),
                    (byte) MathUtil.binPercentage(generatorParameters.getPlateauDensity(), NUM_BINS),
                    (byte) MathUtil.binPercentage(generatorParameters.getMountainDensity(), NUM_BINS),
                    (byte) MathUtil.binPercentage(generatorParameters.getRampDensity(), NUM_BINS),
                    (byte) MathUtil.binPercentage(generatorParameters.getReclaimDensity(), NUM_BINS),
                    (byte) MathUtil.binPercentage(generatorParameters.getMexDensity(), NUM_BINS),
                    (byte) generatorParameters.getSymmetrySettings().getTerrainSymmetry().ordinal()};
        } else if (styleSpecified) {
            int styleIndex = MAP_STYLES.indexOf(MAP_STYLES.stream().filter(styleGenerator -> mapStyle.getName().equals(styleGenerator.getName()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported Map Style")));
            optionArray = new byte[]{(byte) generatorParameters.getSpawnCount(),
                    (byte) (generatorParameters.getMapSize() / 64),
                    (byte) generatorParameters.getNumTeams(),
                    (byte) styleIndex};
        } else {
            optionArray = new byte[]{(byte) generatorParameters.getSpawnCount(),
                    (byte) (generatorParameters.getMapSize() / 64),
                    (byte) generatorParameters.getNumTeams()};
        }
        BitSet parameters = new BitSet();
        parameters.set(0, generatorParameters.isTournamentStyle());
        parameters.set(1, generatorParameters.isBlind());
        parameters.set(2, generatorParameters.isUnexplored());
        String optionString = GeneratedMapNameEncoder.encode(optionArray) + "_" + GeneratedMapNameEncoder.encode(parameters.toByteArray());
        if (generatorParameters.isTournamentStyle()) {
            String timeString = GeneratedMapNameEncoder.encode(ByteBuffer.allocate(8).putLong(generationTime).array());
            optionString += "_" + timeString;
        }
        mapName = String.format(mapNameFormat, VERSION, seedString, optionString).toLowerCase();
    }

    public void save() {
        try {
            long startTime = System.currentTimeMillis();
            MapExporter.exportMap(folderPath, map, !generatorParameters.isTournamentStyle(), true);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

            if (!generatorParameters.isTournamentStyle() && DebugUtil.DEBUG) {
                startTime = System.currentTimeMillis();
                Files.createDirectory(folderPath.resolve(mapName).resolve("debug"));
                SCMapExporter.exportSCMapString(folderPath, mapName, map);
                Pipeline.toFile(folderPath.resolve(mapName).resolve("debug").resolve("pipelineMaskHashes.txt"));
                toFile(folderPath.resolve(mapName).resolve("debug").resolve("generatorParams.txt"));
                System.out.printf("Debug export done: %d ms\n", System.currentTimeMillis() - startTime);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public SCMap generate() throws Exception {
        long startTime = System.currentTimeMillis();
        long sTime = System.currentTimeMillis();

        if (DebugUtil.DEBUG) {
            System.out.printf("Style selection done: %d ms\n", System.currentTimeMillis() - sTime);
        }

        map = mapStyle.generate(generatorParameters, random.nextLong());

        StringBuilder descriptionBuilder = new StringBuilder();
        if (!generatorParameters.isTournamentStyle()) {
            descriptionBuilder.append("Seed: ").append(seed).append("\n");
            descriptionBuilder.append(mapStyle.generatorParameters.toString()).append("\n");
            descriptionBuilder.append("Style: ").append(mapStyle.getName()).append("\n");
            descriptionBuilder.append(mapStyle.generatorsToString()).append("\n");
        } else {
            descriptionBuilder.append(String.format("Map originally generated at %s UTC\n",
                    DateTimeFormatter.ofPattern("HH:mm:ss dd MMM uuuu")
                            .format(Instant.ofEpochSecond(generationTime).atZone(ZoneOffset.UTC))));
        }

        if (generatorParameters.isUnexplored()) {
            map.setCartographicContourInterval(100);
            map.setCartographicDeepWaterColor(1);
            map.setCartographicMapContourColor(1);
            map.setCartographicMapShoreColor(1);
            map.setCartographicMapLandStartColor(1);
            map.setCartographicMapLandEndColor(1);
            descriptionBuilder.append("Use with the Unexplored Maps Mod for best experience");
        }

        map.setDescription(descriptionBuilder.toString().replace("\n", "\\r\\n"));

        int mapSize = map.getSize();
        int compatibleMapSize = (int) StrictMath.pow(2, StrictMath.ceil(StrictMath.log(mapSize) / StrictMath.log(2)));
        Vector2 boundOffset = new Vector2(compatibleMapSize / 2f, compatibleMapSize / 2f);

        map.changeMapSize(mapSize, compatibleMapSize, boundOffset);

        map.addBlank(new Marker(mapName, new Vector2(0, 0)));
        map.addDecalGroup(new DecalGroup(mapName, new int[0]));
        map.setName(mapName);
        map.setFolderName(mapName);
        map.setFilePrefix(mapName);
        ScriptGenerator.generateScript(map);

        System.out.printf("Map generation done: %d ms\n", System.currentTimeMillis() - startTime);

        return map;
    }

    public void toFile(Path path) throws IOException {
        Files.deleteIfExists(path);
        File outFile = path.toFile();
        boolean status = outFile.createNewFile();
        if (status) {
            FileOutputStream out = new FileOutputStream(outFile);
            String summaryString = "Seed: " + seed +
                    "\n" + generatorParameters.toString() +
                    "\nStyle: " + mapStyle.getName() +
                    "\n" + mapStyle.generatorsToString();
            out.write(summaryString.getBytes());
            out.flush();
            out.close();
        }
    }

    @Override
    public Integer call() throws Exception {
        return null;
    }
}
