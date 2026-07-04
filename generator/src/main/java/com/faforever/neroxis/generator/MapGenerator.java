package com.faforever.neroxis.generator;

import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.OutputFolderMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.cli.VisualizeMixin;
import com.faforever.neroxis.exporter.MapExporter;
import com.faforever.neroxis.exporter.SCMapExporter;
import com.faforever.neroxis.exporter.ScriptGenerator;
import com.faforever.neroxis.generator.cli.BasicOptions;
import com.faforever.neroxis.generator.cli.CasualOptions;
import com.faforever.neroxis.generator.cli.CustomStyleOptions;
import com.faforever.neroxis.generator.cli.GenerationOptions;
import com.faforever.neroxis.generator.cli.StyleOptions;
import com.faforever.neroxis.generator.cli.VisibilityOptions;
import com.faforever.neroxis.generator.style.CustomStyleGenerator;
import com.faforever.neroxis.generator.style.StyleGenerator;
import com.faforever.neroxis.map.DecalGroup;
import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.MathUtil;
import com.faforever.neroxis.util.vector.Vector2;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
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
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Getter
@Command(
        name = "generate",
        mixinStandardHelpOptions = true,
        description = "Generates a map from scratch",
        versionProvider = VersionProvider.class,
        usageHelpAutoWidth = true,
        sortOptions = false
)
@NullUnmarked
public class MapGenerator implements Callable<Integer> {
    public static final int NUM_BINS = 127;
    private static final String VERSION = new VersionProvider().getVersion()[0];

    @Spec
    private CommandLine.Model.CommandSpec spec;
    // Set during generation
    private SCMap map;
    private long generationTime;
    private RandomGenerator.SplittableGenerator random;
    private GeneratorParameters generatorParameters;
    private StyleGenerator styleGenerator;
    @CommandLine.Option(
            names = "--map-name",
            order = 1,
            description = "Name of map to recreate. Must be of the form neroxis_map_generator_version_seed_options, if present other parameter options will be ignored"
    )
    private @Nullable String mapName;
    @CommandLine.Option(
            names = "--num-to-generate", order = 2, defaultValue = "1", description = "Number of maps to create"
    )
    private Integer numToGenerate;
    @CommandLine.ArgGroup(exclusive = false)
    private BasicOptions basicOptions = new BasicOptions();
    @CommandLine.ArgGroup
    private GenerationOptions generationOptions = new GenerationOptions();
    @CommandLine.Mixin
    private OutputFolderMixin outputFolderMixin;
    @CommandLine.Mixin
    private DebugMixin debugMixin = new DebugMixin();
    @CommandLine.Mixin
    private VisualizeMixin visualizeMixin = new VisualizeMixin();
    @Option(names = "--preview-path", order = 10000, description = "Folder to save the map previews to")
    private @Nullable Path previewFolder;

    private final boolean dryRun;

    public MapGenerator(boolean dryRun) {
        this.dryRun = dryRun;
    }

    static void main(String[] args) {
        DebugUtil.timedRun("Execution", () -> {
            CommandLine numToGenerateParser = new CommandLine(new MapGenerator(false));
            numToGenerateParser.setAbbreviatedOptionsAllowed(true);
            numToGenerateParser.setUnmatchedArgumentsAllowed(true);
            int numToGenerate = numToGenerateParser.parseArgs(args).matchedOptionValue("num-to-generate", 1);

            for (int i = 0; i < numToGenerate; i++) {
                int exitCode = execute(args);
                if (exitCode != 0) {
                    System.exit(exitCode);
                }
            }
        });
    }

    public static int execute(String[] args) {
        CommandLine commandLine = new CommandLine(new MapGenerator(false));
        commandLine.setAbbreviatedOptionsAllowed(true);
        commandLine.setUnmatchedArgumentsAllowed(true);
        return commandLine.execute(args);
    }

    @Command(
            name = "styles",
            aliases = {"--styles"},
            description = "Prints the map styles available",
            versionProvider = VersionProvider.class,
            usageHelpAutoWidth = true
    )
    private void printStyles() {
        System.out.println(Arrays.stream(MapStyle.Predefined.values())
                                 .map(MapStyle.Predefined::toString)
                                 .collect(Collectors.joining("\n")));
    }

    @Command(
            name = "terrain-styles",
            aliases = {"--terrain-styles"},
            description = "Prints the terrain styles available",
            versionProvider = VersionProvider.class,
            usageHelpAutoWidth = true
    )
    private void printTerrainStyles() {
        System.out.println(
                Arrays.stream(TerrainStyle.values()).map(TerrainStyle::toString).collect(Collectors.joining("\n")));
    }

    @Command(
            name = "texture-styles",
            aliases = {"--texture-styles"},
            description = "Prints the texture styles (the biomes) available",
            versionProvider = VersionProvider.class,
            usageHelpAutoWidth = true
    )
    private void printTextureStyles() {
        System.out.println(
                Arrays.stream(TextureStyle.values()).map(TextureStyle::toString).collect(Collectors.joining("\n")));
    }

    @Command(
            name = "resource-styles",
            aliases = {"--resource-styles"},
            description = "Prints the resource styles available",
            versionProvider = VersionProvider.class,
            usageHelpAutoWidth = true
    )
    private void printResourceStyles() {
        System.out.println(
                Arrays.stream(ResourceStyle.values()).map(ResourceStyle::toString).collect(Collectors.joining("\n")));
    }

    @Command(
            name = "prop-styles",
            aliases = {"--prop-styles"},
            description = "Prints the prop styles available",
            versionProvider = VersionProvider.class,
            usageHelpAutoWidth = true
    )
    private void printPropStyles() {
        System.out.println(
                Arrays.stream(PropStyle.values()).map(PropStyle::toString).collect(Collectors.joining("\n")));
    }

    @Command(
            name = "symmetries",
            aliases = {"--symmetries"},
            description = "Prints the terrain symmetries available",
            versionProvider = VersionProvider.class,
            usageHelpAutoWidth = true
    )
    private void printSymmetries() {
        System.out.println(Arrays.stream(Symmetry.values()).map(Symmetry::toString).collect(Collectors.joining("\n")));
    }

    @Override
    public Integer call() throws Exception {
        Locale.setDefault(Locale.ROOT);

        populateGeneratorParametersAndName();

        assert mapName != null;
        FileUtil.deleteRecursiveIfExists(outputFolderMixin.getOutputPath().resolve(mapName));
        if (!dryRun) {
            System.out.println(mapName);
        }

        generate();
        save();

        Visibility visibility = Optional.ofNullable(generationOptions.getVisibilityOptions())
                                        .map(VisibilityOptions::getVisibility)
                                        .orElse(null);
        if (visibility == null && !dryRun) {
            System.out.printf("Seed: %d%n", basicOptions.getSeed());
            System.out.println(styleGenerator.getGeneratorParameters().toString());
            System.out.printf("Symmetry Settings: %s%n", styleGenerator.getSymmetrySettings());
            System.out.printf("Style: %s%n",
                              generationOptions.getCasualOptions().getStyleOptions().getPredefinedMapStyle());
            System.out.println(styleGenerator.generatorsToString());

            if (previewFolder != null) {
                SCMapExporter.exportPreview(previewFolder, map);
            }
        }

        return 0;
    }

    void populateGeneratorParametersAndName() {
        GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder = GeneratorParameters.builder();
        if (mapName != null) {
            parseMapName(mapName, generatorParametersBuilder);
            populateRequiredGeneratorParameters(generatorParametersBuilder);
        } else {
            checkParameters();
            setVisibility(generatorParametersBuilder);
            populateRequiredGeneratorParameters(generatorParametersBuilder);

            randomizeOptions(generatorParametersBuilder);
        }

        setStyleAndParameters(generatorParametersBuilder);
        encodeMapName();
    }

    private void parseMapName(String mapName,
                              GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        this.mapName = mapName;

        String[] nameArgs = verifyMapName(mapName);

        String seedString = nameArgs[4];
        basicOptions.setSeed(ByteBuffer.wrap(GeneratedMapNameEncoder.decode(seedString)).getLong());

        if (nameArgs.length >= 7) {
            String timeString = nameArgs[6];
            generationTime = ByteBuffer.wrap(GeneratedMapNameEncoder.decode(timeString)).getLong();
        }

        if (nameArgs.length >= 6) {
            String optionString = nameArgs[5];
            parseOptions(GeneratedMapNameEncoder.decode(optionString), generatorParametersBuilder);
        }
    }

    private String[] verifyMapName(String mapName) {
        verifyMapNamePrefix(mapName);
        String[] nameArgs = mapName.split("_");
        verifyNameArgs(mapName, nameArgs);
        return nameArgs;
    }

    private void verifyMapNamePrefix(String mapName) {
        if (!mapName.startsWith("neroxis_map_generator")) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("Map name `%s` is not a generated map", mapName));
        }
    }

    private void verifyNameArgs(String mapName, String[] nameArgs) {
        if (nameArgs.length < 4) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("Map name `%s` does not specify a version",
                                                                   mapName));
        }

        String version = nameArgs[3];
        if (!VERSION.equals(version)) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format(
                    "Version for `%s` does not match this generator version", mapName));
        }

        if (nameArgs.length < 5) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("Map name `%s` does not specify a seed", mapName));
        }
    }

    private void parseOptions(byte[] optionBytes,
                              GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        // The lobby server uses map names with specifically created strings to control the
        // map generation, so we can't assume that the basic options are always present.
        if (optionBytes.length > 0) {
            int spawnCount = optionBytes[0];
            basicOptions.setSpawnCount(spawnCount);
            if (spawnCount <= 16) {
                generatorParametersBuilder.spawnCount(spawnCount);
            }
        }
        if (optionBytes.length > 1) {
            int mapSize = optionBytes[1] * 64;
            basicOptions.setMapSize(mapSize);
            generatorParametersBuilder.mapSize(mapSize);
        }

        if (optionBytes.length > 2) {
            int numTeams = optionBytes[2];
            basicOptions.setNumTeams(numTeams);
            generatorParametersBuilder.numTeams(numTeams);
        }

        randomizeOptions(generatorParametersBuilder);

        if (optionBytes.length == 4 && generationTime != 0) {
            Visibility visibility = Visibility.values()[optionBytes[3]];
            generatorParametersBuilder.visibility(visibility);
        } else if (optionBytes.length > 3) {
            generatorParametersBuilder.terrainSymmetry(Symmetry.values()[optionBytes[3]]);
            if (optionBytes.length == 5) {
                generationOptions.getCasualOptions()
                                 .getStyleOptions()
                                 .setPredefinedMapStyle(MapStyle.Predefined.values()[optionBytes[4]]);
            } else if (optionBytes.length == 10) {
                CustomStyleOptions customStyleOptions = new CustomStyleOptions();
                customStyleOptions.setTextureStyle(TextureStyle.values()[optionBytes[4]]);
                customStyleOptions.setTerrainStyle(TerrainStyle.values()[optionBytes[5]]);
                customStyleOptions.setResourceStyle(ResourceStyle.values()[optionBytes[6]]);
                customStyleOptions.setPropStyle(PropStyle.values()[optionBytes[7]]);
                customStyleOptions.setReclaimDensity(MathUtil.normalizeBin(optionBytes[8], NUM_BINS));
                customStyleOptions.setResourceDensity(MathUtil.normalizeBin(optionBytes[9], NUM_BINS));
                generationOptions.getCasualOptions().getStyleOptions().setCustomStyleOptions(customStyleOptions);
            }
        }
    }

    private void randomizeOptions(GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        random = new SplittableRandom(new SplittableRandom(basicOptions.getSeed()).nextLong() ^
                                      new SplittableRandom(generationTime).nextLong());

        generatorParametersBuilder.terrainSymmetry(getValidTerrainSymmetry());
    }

    private Symmetry getValidTerrainSymmetry() {
        List<Symmetry> terrainSymmetries = switch (basicOptions.getSpawnCount()) {
            case 2, 4, 8 -> new ArrayList<>(
                    Arrays.asList(Symmetry.POINT2, Symmetry.POINT4, Symmetry.POINT6, Symmetry.POINT8, Symmetry.QUAD,
                                  Symmetry.DIAG));
            default -> new ArrayList<>(Arrays.asList(Symmetry.values()));
        };
        terrainSymmetries.remove(Symmetry.X);
        terrainSymmetries.remove(Symmetry.Z);
        if (basicOptions.getNumTeams() > 1) {
            terrainSymmetries.remove(Symmetry.NONE);
            terrainSymmetries.removeIf(symmetry -> symmetry.getNumSymPoints() % basicOptions.getNumTeams() != 0 ||
                                                   symmetry.getNumSymPoints() > basicOptions.getSpawnCount() * 4);
        } else {
            terrainSymmetries.clear();
            terrainSymmetries.add(Symmetry.NONE);
        }
        if (basicOptions.getNumTeams() == 2 && random.nextFloat() < .75f) {
            terrainSymmetries.removeIf(symmetry -> !symmetry.isPerfectSymmetry());
        }
        return terrainSymmetries.get(random.nextInt(terrainSymmetries.size()));
    }

    private void checkParameters() {
        int numTeams = basicOptions.getNumTeams();
        if (numTeams != 0 && basicOptions.getSpawnCount() % numTeams != 0) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("Spawn Count `%d` not a multiple of Num Teams `%d`",
                                                                   basicOptions.getSpawnCount(), numTeams));
        }

        CasualOptions casualOptions = generationOptions.getCasualOptions();

        if (casualOptions.getTerrainSymmetry() != null &&
            numTeams != 0 &&
            casualOptions.getTerrainSymmetry().getNumSymPoints() % numTeams != 0) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format(
                    "Terrain symmetry `%s` not compatible with Num Teams `%d`", casualOptions.getTerrainSymmetry(),
                    numTeams));
        }
    }

    private void setVisibility(GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        if (generationOptions.getVisibilityOptions() != null) {
            generationTime = Instant.now().getEpochSecond();
            basicOptions.setSeed(new SplittableRandom().nextLong());

            Visibility visibility = Optional.ofNullable(generationOptions.getVisibilityOptions())
                                            .map(VisibilityOptions::getVisibility)
                                            .orElse(null);

            generatorParametersBuilder.visibility(visibility);
        }
    }

    private void populateRequiredGeneratorParameters(
            GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        generatorParametersBuilder.mapSize(basicOptions.getMapSize());
        generatorParametersBuilder.numTeams(basicOptions.getNumTeams());
        generatorParametersBuilder.spawnCount(basicOptions.getSpawnCount());
    }

    private void setStyleAndParameters(GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        CasualOptions casualOptions = generationOptions.getCasualOptions();
        if (casualOptions.getTerrainSymmetry() != null) {
            generatorParametersBuilder.terrainSymmetry(casualOptions.getTerrainSymmetry());
        }

        StyleOptions styleOptions = casualOptions.getStyleOptions();
        generatorParameters = generatorParametersBuilder.build();
        if (styleOptions.getPredefinedMapStyle() == null) {
            if (styleOptions.getCustomStyleOptions() != null) {
                setCustomStyle(styleOptions.getCustomStyleOptions());
            } else {
                List<WeightedOption<@NonNull MapStyleGenerator>> generatorOptions = Arrays.stream(
                                                                                                  MapStyle.Predefined.values())
                                                                                          .map(mapStyle -> new WeightedOption<>(
                                                                                                  MapStyleGenerator.of(
                                                                                                          mapStyle),
                                                                                                  mapStyle.getWeight()))
                                                                                          .toList();
                WeightedOptionsWithFallback<@NonNull MapStyleGenerator> styleGeneratorOptions = WeightedOptionsWithFallback.of(
                        MapStyleGenerator.of(MapStyle.Predefined.BASIC), generatorOptions);
                MapStyleGenerator selectedMapStyleGenerator = styleGeneratorOptions.select(random,
                                                                                           mapStyleGenerator -> mapStyleGenerator.styleGenerator()
                                                                                                                                 .getParameterConstraints()
                                                                                                                                 .matches(
                                                                                                                                         generatorParameters));
                styleGenerator = selectedMapStyleGenerator.styleGenerator();
                styleOptions.setPredefinedMapStyle(selectedMapStyleGenerator.mapStyle());
            }
        } else {
            styleGenerator = styleOptions.getPredefinedMapStyle().getGeneratorSupplier().get();
        }
    }

    private void setCustomStyle(CustomStyleOptions customStyleOptions) {
        TextureStyle textureStyle = TextureStyle.values()[random.nextInt(TextureStyle.values().length)];
        TerrainStyle terrainStyle = TerrainStyle.values()[random.nextInt(TerrainStyle.values().length)];
        ResourceStyle resourceStyle = ResourceStyle.values()[random.nextInt(ResourceStyle.values().length)];
        PropStyle propStyle = PropStyle.values()[random.nextInt(PropStyle.values().length)];
        float reclaimDensity = MathUtil.normalizeBin(random.nextInt(NUM_BINS), NUM_BINS);
        float resourceDensity = MathUtil.normalizeBin(random.nextInt(NUM_BINS), NUM_BINS);

        if (customStyleOptions.getTextureStyle() != null) {
            textureStyle = customStyleOptions.getTextureStyle();
        }

        if (customStyleOptions.getTerrainStyle() != null) {
            terrainStyle = customStyleOptions.getTerrainStyle();
        }

        if (customStyleOptions.getResourceStyle() != null) {
            resourceStyle = customStyleOptions.getResourceStyle();
        }

        if (customStyleOptions.getPropStyle() != null) {
            propStyle = customStyleOptions.getPropStyle();
        }

        if (customStyleOptions.getReclaimDensity() != null) {
            reclaimDensity = customStyleOptions.getReclaimDensity();
        }

        if (customStyleOptions.getResourceDensity() != null) {
            resourceDensity = customStyleOptions.getResourceDensity();
        }

        styleGenerator = new MapStyle.Custom(terrainStyle, textureStyle, propStyle, resourceStyle, reclaimDensity,
                                             resourceDensity).getGeneratorSupplier().get();
    }

    private void encodeMapName() {
        if (this.mapName == null) {
            Visibility visibility = generatorParameters.visibility();

            String mapNameFormat = "neroxis_map_generator_%s_%s_%s";
            ByteBuffer seedBuffer = ByteBuffer.allocate(8);
            seedBuffer.putLong(basicOptions.getSeed());
            String seedString = GeneratedMapNameEncoder.encode(seedBuffer.array());
            byte[] optionArray;
            StyleOptions styleOptions = generationOptions.getCasualOptions().getStyleOptions();
            byte spawnOption = (byte) generatorParameters.spawnCount();
            byte mapSizeOption = (byte) (generatorParameters.mapSize() / 64);
            byte numTeamsOption = (byte) generatorParameters.numTeams();
            byte terrainSymmetryOption = (byte) generatorParameters.terrainSymmetry().ordinal();
            if (styleGenerator instanceof CustomStyleGenerator customStyleGenerator) {
                byte textureStyleOption = (byte) customStyleGenerator.getMapStyle().textureStyle().ordinal();
                byte terrainStyleOption = (byte) customStyleGenerator.getMapStyle().terrainStyle().ordinal();
                byte resourceStyleOption = (byte) customStyleGenerator.getMapStyle().resourceStyle().ordinal();
                byte propStyleOption = (byte) customStyleGenerator.getMapStyle().propStyle().ordinal();
                byte reclaimDensityOption = (byte) MathUtil.binPercentage(
                        customStyleGenerator.getMapStyle().reclaimDensity(),
                        NUM_BINS);
                byte resourceDensityOption = (byte) MathUtil.binPercentage(
                        customStyleGenerator.getMapStyle().resourceDensity(),
                        NUM_BINS);
                optionArray = new byte[]{spawnOption, mapSizeOption, numTeamsOption, terrainSymmetryOption, textureStyleOption, terrainStyleOption, resourceStyleOption, propStyleOption, reclaimDensityOption, resourceDensityOption};
            } else if (visibility != null) {
                optionArray = new byte[]{spawnOption, mapSizeOption, numTeamsOption, (byte) visibility.ordinal()};
            } else if (styleOptions.getPredefinedMapStyle() != null) {
                optionArray = new byte[]{spawnOption, mapSizeOption, numTeamsOption, terrainSymmetryOption, (byte) styleOptions.getPredefinedMapStyle()
                                                                                                                               .ordinal()};
            } else {
                optionArray = new byte[]{spawnOption, mapSizeOption, numTeamsOption, terrainSymmetryOption};
            }
            String optionString = GeneratedMapNameEncoder.encode(optionArray);
            if (visibility != null) {
                String timeString = GeneratedMapNameEncoder.encode(
                        ByteBuffer.allocate(8).putLong(generationTime).array());
                optionString += "_" + timeString;
            }
            mapName = String.format(mapNameFormat, VERSION, seedString, optionString).toLowerCase();
        }
    }

    private void generate() {
        long startTime = System.currentTimeMillis();
        long sTime = System.currentTimeMillis();

        if (debugMixin.isDebug()) {
            System.out.printf("Style selection done: %d ms\n", System.currentTimeMillis() - sTime);
            styleGenerator.setDebug(true);
        }

        Visibility visibility = generatorParameters.visibility();
        if (visualizeMixin.isVisualize() && visibility == null) {
            styleGenerator.setVisualize(true);
        }

        map = styleGenerator.generate(generatorParameters, random.split());

        StringBuilder descriptionBuilder = new StringBuilder();
        if (visibility == null) {
            descriptionBuilder.append("Seed: ").append(basicOptions.getSeed()).append("\n");
            descriptionBuilder.append(styleGenerator.getGeneratorParameters().toString()).append("\n");
            if (generationOptions.getCasualOptions().getStyleOptions().getPredefinedMapStyle() != null) {
                descriptionBuilder.append("Style: ")
                                  .append(generationOptions.getCasualOptions()
                                                           .getStyleOptions()
                                                           .getPredefinedMapStyle())
                                  .append("\n");
            } else {
                descriptionBuilder.append("Style: Custom\n");
            }
            descriptionBuilder.append(styleGenerator.generatorsToString()).append("\n");
        } else {
            descriptionBuilder.append(String.format("Map originally generated at %s UTC\n",
                                                    DateTimeFormatter.ofPattern("HH:mm:ss dd MMM uuuu")
                                                                     .format(Instant.ofEpochSecond(generationTime)
                                                                                    .atZone(ZoneOffset.UTC))));
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

        assert mapName != null;
        map.addBlank(new Marker(mapName, new Vector2(0, 0)));
        map.addDecalGroup(new DecalGroup(mapName, List.of()));
        map.setName(mapName);
        map.setFolderName(mapName);
        map.setFilePrefix(mapName);

        ScriptGenerator.generateScript(map);
        if (!dryRun) {
            System.out.printf("Map generation done: %d ms\n", System.currentTimeMillis() - startTime);
        }
    }

    private void save() {
        if (dryRun) {
            return;
        }

        assert mapName != null;
        System.out.printf("Saving map to %s%n", outputFolderMixin.getOutputPath().resolve(mapName).toAbsolutePath());
        try {
            long startTime = System.currentTimeMillis();
            Path outputPath = outputFolderMixin.getOutputPath();
            Visibility visibility = generatorParameters.visibility();
            MapExporter.exportMap(outputPath, map, visibility == null);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

            if (visibility == null && debugMixin.isDebug()) {
                startTime = System.currentTimeMillis();
                Files.createDirectory(outputPath.resolve(mapName).resolve("debug"));
                SCMapExporter.exportSCMapString(outputPath, mapName, map);
                Path path = outputPath.resolve(mapName).resolve("debug").resolve("pipelineMaskHashes.txt");
                Files.deleteIfExists(path);
                File outFile = path.toFile();
                try (FileOutputStream out = new FileOutputStream(outFile)) {
                    styleGenerator.writePipelines(out);
                }
                toFile(outputPath.resolve(mapName).resolve("debug").resolve("generatorParams.txt"));
                System.out.printf("Debug export done: %d ms\n", System.currentTimeMillis() - startTime);
            }
        } catch (IOException e) {
            System.err.println("Error while saving the map.");
            e.printStackTrace();
        }
    }

    private void toFile(Path path) throws IOException {
        Files.deleteIfExists(path);
        File outFile = path.toFile();
        boolean status = outFile.createNewFile();
        if (status) {
            FileOutputStream out = new FileOutputStream(outFile);
            String summaryString = """
                                   Seed: %d
                                   %s
                                   Style: %s
                                   %s"
                                   """.formatted(basicOptions.getSeed(), generatorParameters.toString(),
                                                 generationOptions.getCasualOptions()
                                                                  .getStyleOptions()
                                                                  .getPredefinedMapStyle(),
                                                 styleGenerator.generatorsToString());
            out.write(summaryString.getBytes());
            out.flush();
            out.close();
        }
    }

    private record MapStyleGenerator(MapStyle.Predefined mapStyle, StyleGenerator styleGenerator) {

        private static MapStyleGenerator of(MapStyle.Predefined mapStyle) {
            return new MapStyleGenerator(mapStyle, mapStyle.getGeneratorSupplier().get());
        }

    }
}
