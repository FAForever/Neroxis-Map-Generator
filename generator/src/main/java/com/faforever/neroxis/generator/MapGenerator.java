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
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.MathUtil;
import com.faforever.neroxis.util.Pipeline;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
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
    private Path previewFolder;
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
        private Long seed;
        @Option(names = "--land-density", description = "Land density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private Float landDensity;
        @Option(names = "--plateau-density", description = "Plateau density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private Float plateauDensity;
        @Option(names = "--mountain-density", description = "Mountain density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private Float mountainDensity;
        @Option(names = "--ramp-density", description = "Ramp density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private Float rampDensity;
        @Option(names = "--reclaim-density", description = "Reclaim density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private Float reclaimDensity;
        @Option(names = "--mex-density", description = "Mex density for the generated map. Min: 0 Max: 1", parameterConsumer = DensityParameterConsumer.class)
        private Float mexDensity;
        @Option(names = "--terrain-symmetry", description = "Base terrain symmetry for the map")
        private Symmetry terrainSymmetry;
        @Option(names = "--biome", description = "Texture biome for the generated map", parameterConsumer = BiomeParameterConsumer.class)
        private Biome biome;
    }

    @Getter
    @SuppressWarnings("unused")
    static strictfp class VisibilityOptions {
        private boolean tournamentStyle;
        private boolean blind;
        private boolean unexplored;

        @Option(names = "--tournament-style", description = "Remove the preview.png and add time of original generation to map")
        public void setTournamentStyle(boolean value) {
            this.tournamentStyle = value;
        }

        @Option(names = "--blind", description = "Remove the preview.png, add time of original generation to map, and remove in game lobby preview")
        public void setBlind(boolean value) {
            this.tournamentStyle = value;
            this.blind = value;
        }

        @Option(names = "--unexplored", description = "Remove the preview.png, add time of original generation to map, remove in game lobby preview, and add unexplored fog of war")
        public void setUnexplored(boolean value) {
            this.tournamentStyle = value;
            this.blind = value;
            this.unexplored = value;
        }
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

    @Option(names = "--debug", description = "Enable debugging")
    public void setDebugging(boolean debug) {
        DebugUtil.DEBUG = debug;
        Pipeline.HASH_MASK = debug;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new MapGenerator()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        initialize();

        Locale.setDefault(Locale.ROOT);
        Pipeline.reset();
        seed = new Random().nextLong();

        if (options.mapName != null) {
            parseMapName(options.mapName);
        } else {
            randomizeOptions();

            MapParameters mapParameters = options.mapParameters;
            ParameterOptions parameterOptions = mapParameters.mapSpecifications.parameterOptions;
            if (parameterOptions != null) {
                if (parameterOptions.biome != null) {
                    generatorParametersBuilder.biome(parameterOptions.biome);
                }
                if (parameterOptions.biome != null) {
                    generatorParametersBuilder.landDensity(parameterOptions.landDensity);
                }
                if (parameterOptions.biome != null) {
                    generatorParametersBuilder.plateauDensity(parameterOptions.plateauDensity);
                }
                if (parameterOptions.biome != null) {
                    generatorParametersBuilder.mountainDensity(parameterOptions.mountainDensity);
                }
                if (parameterOptions.biome != null) {
                    generatorParametersBuilder.rampDensity(parameterOptions.rampDensity);
                }
                if (parameterOptions.biome != null) {
                    generatorParametersBuilder.reclaimDensity(parameterOptions.reclaimDensity);
                }
                if (parameterOptions.biome != null) {
                    generatorParametersBuilder.mexDensity(parameterOptions.mexDensity);
                }
                if (parameterOptions.biome != null) {
                    generatorParametersBuilder.terrainSymmetry(parameterOptions.terrainSymmetry);
                }
            } else if (mapParameters.mapSpecifications.mapStyle != null) {
                GeneratorParameters tempGeneratorParameters = generatorParametersBuilder.build();
                mapStyle = mapParameters.mapSpecifications.mapStyle;
                generatorParameters = mapStyle.getParameterConstraints().initParameters(random, mapParameters.spawnCount, mapParameters.mapSize, mapParameters.numTeams, false, false, false, tempGeneratorParameters.getTerrainSymmetry());
            } else {
                GeneratorParameters tempGeneratorParameters = generatorParametersBuilder.build();
                mapStyle = StyleGenerator.selectRandomMatchingGenerator(random, PRODUCTION_STYLES, mapParameters.spawnCount, mapParameters.mapSize, mapParameters.numTeams, new BasicStyleGenerator());
                VisibilityOptions visibilityOptions = mapParameters.mapSpecifications.visibilityOptions;
                generatorParameters = mapStyle.getParameterConstraints().initParameters(random, mapParameters.spawnCount, mapParameters.mapSize, mapParameters.numTeams, visibilityOptions.tournamentStyle, visibilityOptions.blind, visibilityOptions.unexplored, tempGeneratorParameters.getTerrainSymmetry());
            }
            generateMapName();
        }

        FileUtil.deleteRecursiveIfExists(folderPath.resolve(mapName));
        System.out.println(mapName);
        generate();
        if (map == null) {
            System.out.println("Map Generation Failed see stack trace for details");
            return 1;
        }
        save();

        System.out.printf("Saving map to %s%n", folderPath.resolve(mapName).toAbsolutePath());
        System.out.printf("Seed: %d%n", seed);
        System.out.println(mapStyle.generatorParameters.toString());
        System.out.printf("Style: %s%n", mapStyle.getName());
        System.out.println(mapStyle.generatorsToString());
        if (!options.getMapParameters().getMapSpecifications().visibilityOptions.tournamentStyle && previewFolder != null) {
            SCMapExporter.exportPreview(previewFolder, map);
        }

        Pipeline.shutdown();

        return 0;
    }

    private void initialize() {
        if (options.mapParameters.spawnCount % options.mapParameters.numTeams != 0) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("Spawn Count `%d` not a multiple of Num Teams `%d`", options.mapParameters.spawnCount, options.mapParameters.numTeams)
            );
        }

        if (options.mapParameters.mapSpecifications != null
                && options.mapParameters.mapSpecifications.visibilityOptions != null
                && options.mapParameters.mapSpecifications.visibilityOptions.tournamentStyle) {
            generationTime = Instant.now().getEpochSecond();
        }
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
        int spawnCount = tempGeneratorParameters.getSpawnCount();
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

    private void parseMapName(String mapName) throws Exception {
        if (!mapName.startsWith("neroxis_map_generator")) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("Map name `%s` is not a generated map", mapName)
            );
        }

        String[] nameArgs = mapName.split("_");
        if (nameArgs.length < 4) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("Map name `%s` does not specify a version", mapName)
            );
        }

        String version = nameArgs[3];
        if (!VERSION.equals(version)) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("Version for `%s` does not match this generator version", mapName)
            );
        }

        if (nameArgs.length < 5) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("Map name `%s` does not specify a seed", mapName)
            );
        }

        this.mapName = mapName;

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
        if (options.mapParameters.mapSpecifications.parameterOptions != null) {
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
                    (byte) generatorParameters.getTerrainSymmetry().ordinal()};
        } else if (options.mapParameters.mapSpecifications.mapStyle != null) {
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
}
