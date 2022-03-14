package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.cli.CLIUtils;
import com.faforever.neroxis.generator.cli.ParameterOptions;
import com.faforever.neroxis.generator.cli.VersionProvider;
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
@Command(name = "generate", mixinStandardHelpOptions = true, description = "Generates a map from scratch",
        versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public strictfp class MapGenerator implements Callable<Integer> {

    private static final String VERSION = new VersionProvider().getVersion()[0];
    public static final int NUM_BINS = 127;
    private static final List<StyleGenerator> MAP_STYLES = List.of(new BigIslandsStyleGenerator(), new CenterLakeStyleGenerator(), new BasicStyleGenerator(), new DropPlateauStyleGenerator(), new LandBridgeStyleGenerator(), new LittleMountainStyleGenerator(), new MountainRangeStyleGenerator(), new OneIslandStyleGenerator(), new SmallIslandsStyleGenerator(), new ValleyStyleGenerator(), new HighReclaimStyleGenerator(), new LowMexStyleGenerator(), new FloodedStyleGenerator(), new TestStyleGenerator());
    private static final List<StyleGenerator> PRODUCTION_STYLES = MAP_STYLES.stream().filter(styleGenerator -> !(styleGenerator instanceof TestStyleGenerator)).collect(Collectors.toList());

    @Spec
    private CommandLine.Model.CommandSpec spec;

    //Set during generation
    private SCMap map;
    private long seed;
    private long generationTime;
    private Random random;
    private GeneratorParameters generatorParameters;
    private GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder = GeneratorParameters.builder();

    //read from cli args
    private Path folderPath;
    private Path previewFolder;
    private String mapName;
    private int spawnCount;
    private int mapSize;
    private int numTeams;
    private Visibility visibility;
    private StyleGenerator mapStyle;
    @ArgGroup(heading = "Options that control map generation%n")
    private ParameterOptions parameterOptions;

    @Option(names = {"--folder-path"}, defaultValue = ".", description = "Folder to save the map in", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    public void setFolderPath(Path folderPath) throws IOException {
        CLIUtils.checkWritableDirectory(folderPath, spec);
        this.folderPath = folderPath;
    }

    @Option(names = {"--preview-path"}, description = "Folder to save the map previews to")
    public void setPreviewFolder(Path previewFolder) throws IOException {
        CLIUtils.checkWritableDirectory(previewFolder, spec);
        this.previewFolder = previewFolder;
    }

    @Option(names = {"--map-name"}, description = "Name of map to recreate. Must be of the form neroxis_map_generator_version_seed_options")
    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    @Option(names = "--spawn-count", defaultValue = "6", description = "Spawn count for the generated map", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    public void setSpawnCount(int spawnCount) {
        this.spawnCount = spawnCount;
    }

    @Option(names = "--map-size", defaultValue = "512", description = "Generated map size, can be specified in oGrids (e.g 512) or km (e.g 10km)", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    public void setMapSize(String mapSizeString) {
        this.mapSize = CLIUtils.convertMapSizeString(mapSizeString, spec);
    }

    @Option(names = "--num-teams", defaultValue = "2", description = "Number of teams for the generated map (0 is no teams asymmetric)", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    public void setNumTeams(int numTeams) {
        this.numTeams = numTeams;
    }

    @Option(names = "--visibility", description = "Visibility for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    @Option(names = "--tournament-style", hidden = true, description = "Remove the preview.png and add time of original generation to map")
    public void setTournamentStyle(boolean value) {
        this.visibility = Visibility.TOURNAMENT;
    }

    @Option(names = "--blind", hidden = true, description = "Remove the preview.png, add time of original generation to map, and remove in game lobby preview")
    public void setBlind(boolean value) {
        this.visibility = Visibility.BLIND;
    }

    @Option(names = "--unexplored", hidden = true, description = "Remove the preview.png, add time of original generation to map, remove in game lobby preview, and add unexplored fog of war")
    public void setUnexplored(boolean value) {
        this.visibility = Visibility.UNEXPLORED;
    }

    @Option(names = "--style", description = "Style for the generated map")
    public void setMapStyle(String style) {
        mapStyle = MAP_STYLES.stream()
                .filter(mapStyle -> mapStyle.getName().equals(style.toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported Map Style"));
    }

    @Option(names = "--debug", description = "Enable debugging")
    public void setDebugging(boolean debug) {
        DebugUtil.DEBUG = debug;
        Pipeline.HASH_MASK = debug;
    }

    @Command(name = "styles", aliases = {"--styles"}, description = "Prints the styles available",
            versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
    public void printStyles() {
        System.out.println(PRODUCTION_STYLES.stream().map(StyleGenerator::getName).collect(Collectors.joining("\n")));
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        int exitCode = new CommandLine(new MapGenerator()).execute(args);
        System.out.printf("Execution done: %d ms\n", System.currentTimeMillis() - startTime);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Locale.setDefault(Locale.ROOT);
        Pipeline.reset();
        seed = new Random().nextLong();

        if (mapName != null) {
            parseMapName(mapName);
        } else {
            checkParameters();
            setGenerationTimeIfNecessary();
            populateRequiredGeneratorParametersFromOptions();

            randomizeOptions();
        }

        setStyleAndParameters();
        generateMapName();

        FileUtil.deleteRecursiveIfExists(folderPath.resolve(mapName));
        System.out.println(mapName);

        generate();
        save();

        System.out.printf("Saving map to %s%n", folderPath.resolve(mapName).toAbsolutePath());

        if (visibility == null) {
            System.out.printf("Seed: %d%n", seed);
            System.out.println(mapStyle.generatorParameters.toString());
            System.out.printf("Style: %s%n", mapStyle.getName());
            System.out.println(mapStyle.generatorsToString());

            if (previewFolder != null) {
                SCMapExporter.exportPreview(previewFolder, map);
            }
        }

        Pipeline.shutdown();
        return 0;
    }

    private void setStyleAndParameters() {
        if (mapStyle == null) {
            overwriteOptionalGeneratorParametersFromOptions();
            generatorParameters = generatorParametersBuilder.build();
            mapStyle = StyleGenerator.selectRandomMatchingGenerator(random, PRODUCTION_STYLES, generatorParameters, new BasicStyleGenerator());
        } else {
            generatorParameters = mapStyle.getParameterConstraints().initParameters(random, generatorParametersBuilder);
        }
    }

    private void populateRequiredGeneratorParametersFromOptions() {
        generatorParametersBuilder.mapSize(mapSize);
        generatorParametersBuilder.numTeams(numTeams);
        generatorParametersBuilder.spawnCount(spawnCount);
    }

    private void overwriteOptionalGeneratorParametersFromOptions() {
        if (parameterOptions != null) {
            if (parameterOptions.getBiome() != null) {
                generatorParametersBuilder.biome(parameterOptions.getBiome());
            }
            if (parameterOptions.getLandDensity() != null) {
                generatorParametersBuilder.landDensity(parameterOptions.getLandDensity());
            }
            if (parameterOptions.getPlateauDensity() != null) {
                generatorParametersBuilder.plateauDensity(parameterOptions.getPlateauDensity());
            }
            if (parameterOptions.getMountainDensity() != null) {
                generatorParametersBuilder.mountainDensity(parameterOptions.getMountainDensity());
            }
            if (parameterOptions.getRampDensity() != null) {
                generatorParametersBuilder.rampDensity(parameterOptions.getRampDensity());
            }
            if (parameterOptions.getReclaimDensity() != null) {
                generatorParametersBuilder.reclaimDensity(parameterOptions.getReclaimDensity());
            }
            if (parameterOptions.getMexDensity() != null) {
                generatorParametersBuilder.mexDensity(parameterOptions.getMexDensity());
            }
            if (parameterOptions.getTerrainSymmetry() != null) {
                generatorParametersBuilder.terrainSymmetry(parameterOptions.getTerrainSymmetry());
            }
        }
    }

    private void setGenerationTimeIfNecessary() {
        if (visibility != null) {
            generationTime = Instant.now().getEpochSecond();
        }
    }

    private void checkParameters() {
        if (spawnCount % numTeams != 0) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("Spawn Count `%d` not a multiple of Num Teams `%d`", spawnCount, numTeams)
            );
        }

        if (parameterOptions != null && parameterOptions.getTerrainSymmetry() != null && numTeams % parameterOptions.getTerrainSymmetry().getNumSymPoints() != 0) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("Terrain symmetry `%s` not compatible with Num Teams `%d`", parameterOptions.getTerrainSymmetry(), numTeams)
            );
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

    private void parseMapName(String mapName) {
        this.mapName = mapName;

        String[] nameArgs = verifyMapName(mapName);

        String seedString = nameArgs[4];
        byte[] seedBytes = GeneratedMapNameEncoder.decode(seedString);
        ByteBuffer seedWrapper = ByteBuffer.wrap(seedBytes);
        seed = seedWrapper.getLong();

        if (nameArgs.length >= 7) {
            String timeString = nameArgs[6];
            generationTime = ByteBuffer.wrap(GeneratedMapNameEncoder.decode(timeString)).getLong();
        }

        if (nameArgs.length >= 6) {
            String optionString = nameArgs[5];
            parseOptions(GeneratedMapNameEncoder.decode(optionString));
        }
    }

    private String[] verifyMapName(String mapName) {
        verifyMapNamePrefix(mapName);
        String[] nameArgs = mapName.split("_");
        verifyNameArgs(mapName, nameArgs);
        return nameArgs;
    }

    private void verifyNameArgs(String mapName, String[] nameArgs) {
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
    }

    private void verifyMapNamePrefix(String mapName) {
        if (!mapName.startsWith("neroxis_map_generator")) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    String.format("Map name `%s` is not a generated map", mapName)
            );
        }
    }

    private void parseOptions(byte[] optionBytes) {
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
            if (generationTime == 0) {
                mapStyle = MAP_STYLES.get(optionBytes[3]);
            } else {
                visibility = Visibility.values()[optionBytes[3]];
            }
        }
    }


    private void randomizeOptions() {
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
        CommandLine.ParseResult parseResult = spec.commandLine().getParseResult();

        String mapNameFormat = "neroxis_map_generator_%s_%s_%s";
        ByteBuffer seedBuffer = ByteBuffer.allocate(8);
        seedBuffer.putLong(seed);
        String seedString = GeneratedMapNameEncoder.encode(seedBuffer.array());
        byte[] optionArray;
        if (parameterOptions != null) {
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
        } else if (visibility != null) {
            optionArray = new byte[]{(byte) generatorParameters.getSpawnCount(),
                    (byte) (generatorParameters.getMapSize() / 64),
                    (byte) generatorParameters.getNumTeams(),
                    (byte) visibility.ordinal()};
        } else if (parseResult.hasMatchedOption("--style")) {
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
        String optionString = GeneratedMapNameEncoder.encode(optionArray);
        if (visibility != null) {
            String timeString = GeneratedMapNameEncoder.encode(ByteBuffer.allocate(8).putLong(generationTime).array());
            optionString += "_" + timeString;
        }
        String mapName = String.format(mapNameFormat, VERSION, seedString, optionString).toLowerCase();

        if (this.mapName != null && !this.mapName.equals(mapName)) {
            throw new IllegalStateException(String.format("Mapname not reproduced: Original name `%s` This name `%s`", this.mapName, mapName));
        }

        this.mapName = mapName;
    }

    public void save() {
        try {
            long startTime = System.currentTimeMillis();
            MapExporter.exportMap(folderPath, map, visibility == null, true);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

            if (visibility == null && DebugUtil.DEBUG) {
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
        if (visibility == null) {
            descriptionBuilder.append("Seed: ").append(seed).append("\n");
            descriptionBuilder.append(mapStyle.generatorParameters.toString()).append("\n");
            descriptionBuilder.append("Style: ").append(mapStyle.getName()).append("\n");
            descriptionBuilder.append(mapStyle.generatorsToString()).append("\n");
        } else {
            descriptionBuilder.append(String.format("Map originally generated at %s UTC\n",
                    DateTimeFormatter.ofPattern("HH:mm:ss dd MMM uuuu")
                            .format(Instant.ofEpochSecond(generationTime).atZone(ZoneOffset.UTC))));
        }

        if (Visibility.UNEXPLORED == visibility) {
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
