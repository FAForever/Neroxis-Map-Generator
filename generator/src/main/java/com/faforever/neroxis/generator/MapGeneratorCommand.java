package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.OutputFolderMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.cli.VisualizeMixin;
import com.faforever.neroxis.exporter.MapExporter;
import com.faforever.neroxis.exporter.SCMapExporter;
import com.faforever.neroxis.exporter.ScriptGenerator;
import com.faforever.neroxis.generator.cli.BasicOptions;
import com.faforever.neroxis.generator.cli.CustomStyleOptions;
import com.faforever.neroxis.generator.cli.GenerationOptions;
import com.faforever.neroxis.generator.cli.SpecifiedOptions;
import com.faforever.neroxis.generator.cli.StyleOptions;
import com.faforever.neroxis.generator.cli.TopLevelOptions;
import com.faforever.neroxis.generator.cli.VisibilityOptions;
import com.faforever.neroxis.generator.util.serial.GeneratedMapNameEncoder;
import com.faforever.neroxis.generator.util.serial.GeneratorParseOutput;
import com.faforever.neroxis.generator.util.serial.MapNameParameters;
import com.faforever.neroxis.generator.util.serial.MapStyle;
import com.faforever.neroxis.generator.util.serial.PropStyle;
import com.faforever.neroxis.generator.util.serial.ResourceStyle;
import com.faforever.neroxis.generator.util.serial.TerrainStyle;
import com.faforever.neroxis.generator.util.serial.Visibility;
import com.faforever.neroxis.map.DecalGroup;
import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.MathUtil;
import com.faforever.neroxis.util.SymmetrySelector;
import com.faforever.neroxis.util.vector.Vector2;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

@Command(
        name = "generate",
        mixinStandardHelpOptions = true,
        description = "Generates a map from scratch",
        versionProvider = VersionProvider.class,
        usageHelpAutoWidth = true,
        sortOptions = false,
        scope = CommandLine.ScopeType.INHERIT
)
public class MapGeneratorCommand implements Callable<Integer> {

    private static final WeightedOptionsWithFallback<MapStyle.Predefined> RANDOM_MAP_STYLE_OPTIONS = WeightedOptionsWithFallback.of(
            MapStyle.Predefined.BASIC, WeightedOption.of(MapStyle.Predefined.BASIC, 2),
            WeightedOption.of(MapStyle.Predefined.BIG_ISLANDS, 1),
            WeightedOption.of(MapStyle.Predefined.CENTER_LAKE, 1),
            WeightedOption.of(MapStyle.Predefined.DROP_PLATEAU, .5f),
            WeightedOption.of(MapStyle.Predefined.FLOODED, .5f),
            WeightedOption.of(MapStyle.Predefined.HIGH_RECLAIM, .25f),
            WeightedOption.of(MapStyle.Predefined.LAND_BRIDGE, 2),
            WeightedOption.of(MapStyle.Predefined.LITTLE_MOUNTAIN, 1),
            WeightedOption.of(MapStyle.Predefined.LOW_MEX, .5f),
            WeightedOption.of(MapStyle.Predefined.MOUNTAIN_RANGE, 1),
            WeightedOption.of(MapStyle.Predefined.MULTILEVEL, 1f), WeightedOption.of(MapStyle.Predefined.ONE_ISLAND, 1),
            WeightedOption.of(MapStyle.Predefined.SMALL_ISLANDS, 1), WeightedOption.of(MapStyle.Predefined.VALLEY, 1),
            WeightedOption.of(MapStyle.Predefined.RIVERS, .25f),
            WeightedOption.of(MapStyle.Predefined.RIVERS_AND_OCEANS, .25f),
            WeightedOption.of(MapStyle.Predefined.FRACTAL_LAND, 1f),
            WeightedOption.of(MapStyle.Predefined.FRACTAL_PLATEAU, .25f),
            WeightedOption.of(MapStyle.Predefined.FRACTAL_NAVY, .75f),
            WeightedOption.of(MapStyle.Predefined.SETONISH, 1),
            WeightedOption.of(MapStyle.Predefined.FORREST_SOMETHING, .01f));
    private final RandomGenerator random = new SplittableRandom();
    @Spec
    @SuppressWarnings("NullAway")
    private CommandLine.Model.CommandSpec spec;
    @CommandLine.ArgGroup
    private TopLevelOptions topLevelOptions = new TopLevelOptions();
    @Option(
            names = "--parse", description = "Only parse the options and return the parameters in json"
    )
    private boolean parse;
    @CommandLine.Mixin
    private DebugMixin debugMixin = new DebugMixin();
    @CommandLine.Mixin
    private VisualizeMixin visualizeMixin = new VisualizeMixin();
    @Option(names = "--preview-path", order = 10000, description = "Folder to save the map previews to")
    private @Nullable Path previewFolder;
    @CommandLine.Mixin
    private OutputFolderMixin outputFolderMixin = new OutputFolderMixin();

    static void main(String[] args) {
        System.exit(DebugUtil.timedRun("Execution", () -> execute(args)));
    }

    public static int execute(String[] args) {
        CommandLine commandLine = new CommandLine(new MapGeneratorCommand());
        commandLine.setAbbreviatedOptionsAllowed(true);
        commandLine.setUnmatchedArgumentsAllowed(true);
        return commandLine.execute(args);
    }

    static GenerationResults generate(MapNameParameters mapNameParameters, boolean debug, boolean visualize) {
        RandomGenerator.SplittableGenerator random = mapNameParameters.createRandom();
        MapStyle mapStyle;
        if (mapNameParameters.mode() instanceof MapNameParameters.Casual(_, MapStyle specifiedMapStyle) &&
            specifiedMapStyle != null) {
            mapStyle = specifiedMapStyle;
        } else {
            mapStyle = RANDOM_MAP_STYLE_OPTIONS.select(random.split(),
                                                       mapStyle1 -> mapStyle1.parameterConstraints()
                                                                             .matches(mapNameParameters));
        }
        String mapName = GeneratedMapNameEncoder.encode(mapNameParameters);
        SymmetrySettings symmetrySettings = switch (mapNameParameters.mode()) {
            case MapNameParameters.Casual(Symmetry terrainSymmetry, _) when terrainSymmetry != null ->
                    SymmetrySelector.getSymmetrySettingsFromTerrainSymmetry(
                            random.split(), terrainSymmetry, mapNameParameters.spawnCount(),
                            mapNameParameters.numTeams());
            default -> SymmetrySelector.getSymmetrySettings(random.split(),
                                                            mapNameParameters.spawnCount(),
                                                            mapNameParameters.numTeams());
        };
        if (mapStyle == MapStyle.Predefined.SETONISH) {
            symmetrySettings = switch (symmetrySettings.terrainSymmetry()) {
                case Symmetry.ZX -> new SymmetrySettings(Symmetry.ZX, Symmetry.ZX, Symmetry.ZX);
                case Symmetry.X -> new SymmetrySettings(Symmetry.X, Symmetry.X, Symmetry.X);
                case Symmetry.Z -> new SymmetrySettings(Symmetry.Z, Symmetry.Z, Symmetry.Z);
                default -> new SymmetrySettings(Symmetry.POINT2, Symmetry.XZ, Symmetry.POINT2);
            };
        }

        Visibility visibility = switch (mapNameParameters.mode()) {
            case MapNameParameters.Competitive(_, Visibility visibilityParameter) -> visibilityParameter;
            default -> null;
        };

        GeneratorParameters generatorParameters = new GeneratorParameters(mapNameParameters.spawnCount(),
                                                                          mapNameParameters.mapSize(),
                                                                          mapNameParameters.numTeams(), mapStyle,
                                                                          symmetrySettings,
                                                                          visibility);

        long startTime = System.currentTimeMillis();

        GenerationResults generationResults = MapGenerator.generate(generatorParameters, random.split(),
                                                                    debug && mapNameParameters.allowDebug(),
                                                                    visualize && mapNameParameters.allowDebug());
        SCMap map = generationResults.map();

        StringBuilder descriptionBuilder = new StringBuilder();
        if (!(mapNameParameters.mode() instanceof MapNameParameters.Competitive(long generationTime, _))) {
            descriptionBuilder.append("Seed: ").append(mapNameParameters.seed()).append("\n");
            descriptionBuilder.append(mapNameParameters).append("\n");
            descriptionBuilder.append("Style: ").append(mapStyle.name()).append("\n");
            descriptionBuilder.append(generationResults.settingsToString()).append("\n");
        } else {
            descriptionBuilder.append(String.format("Map originally generated at %s UTC\n",
                                                    DateTimeFormatter.ofPattern("HH:mm:ss dd MMM uuuu")
                                                                     .format(Instant.ofEpochSecond(generationTime)
                                                                                    .atZone(ZoneOffset.UTC))));
        }

        if (visibility == Visibility.UNEXPLORED) {
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
        map.addDecalGroup(new DecalGroup(mapName, List.of()));
        map.setName(mapName);
        map.setFolderName(mapName);
        map.setFilePrefix(mapName);

        ScriptGenerator.generateScript(map);

        System.out.printf("Map generation done: %d ms\n", System.currentTimeMillis() - startTime);

        return generationResults;
    }

    private static void save(GenerationResults generationResults, MapNameParameters mapNameParameters,
                             Path outputPath, boolean debug) {
        SCMap map = generationResults.map();
        FileUtil.deleteRecursiveIfExists(outputPath.resolve(map.getFolderName()));
        System.out.printf("Saving map to %s%n", outputPath.resolve(map.getFolderName()).toAbsolutePath());
        try {
            long startTime = System.currentTimeMillis();
            MapExporter.exportMap(outputPath, map, mapNameParameters.allowDebug());
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

            if (mapNameParameters.allowDebug()) {
                if (debug) {
                    startTime = System.currentTimeMillis();
                    Path debugFolder = outputPath.resolve(map.getFolderName()).resolve("debug");
                    Files.createDirectory(debugFolder);
                    SCMapExporter.exportSCMapString(outputPath, map.getName(), map);
                    System.out.printf("Debug export done: %d ms\n", System.currentTimeMillis() - startTime);
                    Path path = debugFolder.resolve("pipelineMaskHashes.txt");
                    Files.deleteIfExists(path);
                    File outFile = path.toFile();
                    try (FileOutputStream out = new FileOutputStream(outFile)) {
                        generationResults.writePipelines(out);
                    }
                }

                System.out.println(mapNameParameters);
                System.out.println(generationResults.settingsToString());
            }
        } catch (IOException e) {
            System.err.println("Error while saving the map.");
            e.printStackTrace();
        }
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
            name = "biomes",
            aliases = {"--texture-styles", "--biomes"},
            description = "Prints the biomes available",
            versionProvider = VersionProvider.class,
            usageHelpAutoWidth = true
    )
    private void printBiomes() {
        System.out.println(
                Arrays.stream(BiomeName.values()).map(BiomeName::toString).collect(Collectors.joining("\n")));
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

        int numToGenerate = topLevelOptions.getSpecifiedOptions().getNumToGenerate();
        for (int i = 0; i < numToGenerate; i++) {
            MapNameParameters mapNameParameters = createMapNameParameters();
            if (parse) {
                FileUtil.serialize(System.out, new GeneratorParseOutput(mapNameParameters));
                break;
            }

            System.out.println(GeneratedMapNameEncoder.encode(mapNameParameters));

            GenerationResults generationResults = generate(mapNameParameters, debugMixin.isDebug(),
                                                           visualizeMixin.isVisualize());

            save(generationResults, mapNameParameters, outputFolderMixin.getOutputPath(), debugMixin.isDebug());

            if (mapNameParameters.allowDebug() && previewFolder != null) {
                SCMapExporter.exportPreview(previewFolder, generationResults.map());
            }
        }

        return 0;
    }

    MapNameParameters createMapNameParameters() {
        if (topLevelOptions.getSpecifiedMapName() != null) {
            try {
                return GeneratedMapNameEncoder.decode(topLevelOptions.getSpecifiedMapName());
            } catch (MapNameException e) {
                throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage(), e);
            }
        }

        return createGeneratorParametersFromOptions(topLevelOptions.getSpecifiedOptions());
    }

    private MapNameParameters createGeneratorParametersFromOptions(SpecifiedOptions specifiedOptions) {
        checkParameters(specifiedOptions);

        BasicOptions basicOptions = specifiedOptions.getBasicOptions();
        GenerationOptions generationOptions = specifiedOptions.getGenerationOptions();

        boolean competitive = generationOptions.getVisibilityOptions() != null;
        long seed = competitive ||
                    specifiedOptions.getNumToGenerate() > 1 ||
                    generationOptions.getCasualOptions().getSeed() ==
                    null ? random.nextLong() : generationOptions.getCasualOptions().getSeed();
        int spawnCount = basicOptions.getSpawnCount();
        int mapSize = basicOptions.getMapSize();
        int numTeams = basicOptions.getNumTeams();
        Symmetry terrainSymmetry = generationOptions.getCasualOptions().getTerrainSymmetry();
        Visibility visibility = Optional.ofNullable(generationOptions.getVisibilityOptions())
                                        .map(VisibilityOptions::getVisibility)
                                        .orElse(null);

        MapNameParameters.Mode mode = visibility == null ? new MapNameParameters.Casual(terrainSymmetry,
                                                                                        createMapStyle(
                                                                                                generationOptions)) : new MapNameParameters.Competitive(
                Instant.now().getEpochSecond(), visibility);

        return new MapNameParameters(seed, spawnCount, mapSize, numTeams, mode);
    }

    private void checkParameters(SpecifiedOptions specifiedOptions) {
        BasicOptions basicOptions = specifiedOptions.getBasicOptions();

        int numTeams = basicOptions.getNumTeams();
        int spawnCount = basicOptions.getSpawnCount();
        if (numTeams != 0 && spawnCount % numTeams != 0) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                                                     String.format("Spawn Count `%d` not a multiple of Num Teams `%d`",
                                                                   spawnCount, numTeams));
        }

        GenerationOptions generationOptions = specifiedOptions.getGenerationOptions();
        Symmetry terrainSymmetry = generationOptions.getCasualOptions().getTerrainSymmetry();
        if (numTeams != 0 && terrainSymmetry != null && terrainSymmetry.getNumSymPoints() % numTeams != 0) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format(
                    "Terrain symmetry `%s` not compatible with Num Teams `%d`", terrainSymmetry, numTeams));
        }
    }

    private @Nullable MapStyle createMapStyle(GenerationOptions generationOptions) {
        if (generationOptions.getCasualOptions().getStyleOptions() != null) {
            StyleOptions styleOptions = generationOptions.getCasualOptions().getStyleOptions();
            if (styleOptions.getCustomStyleOptions() != null) {
                return createCustomStyle(styleOptions.getCustomStyleOptions());
            } else if (styleOptions.getPredefinedMapStyle() != null) {
                return styleOptions.getPredefinedMapStyle();
            }
        }

        return null;
    }

    private MapStyle.Custom createCustomStyle(CustomStyleOptions customStyleOptions) {
        BiomeName biomeName;
        TerrainStyle terrainStyle;
        ResourceStyle resourceStyle;
        PropStyle propStyle;
        float reclaimDensity;
        float resourceDensity;

        if (customStyleOptions.getBiomeName() != null) {
            biomeName = customStyleOptions.getBiomeName();
        } else {
            biomeName = BiomeName.values()[random.nextInt(BiomeName.values().length)];
        }

        if (customStyleOptions.getTerrainStyle() != null) {
            terrainStyle = customStyleOptions.getTerrainStyle();
        } else {
            terrainStyle = TerrainStyle.values()[random.nextInt(TerrainStyle.values().length)];
        }

        if (customStyleOptions.getResourceStyle() != null) {
            resourceStyle = customStyleOptions.getResourceStyle();
        } else {
            resourceStyle = ResourceStyle.values()[random.nextInt(ResourceStyle.values().length)];
        }

        if (customStyleOptions.getPropStyle() != null) {
            propStyle = customStyleOptions.getPropStyle();
        } else {
            propStyle = PropStyle.values()[random.nextInt(PropStyle.values().length)];
        }

        if (customStyleOptions.getReclaimDensity() != null) {
            reclaimDensity = customStyleOptions.getReclaimDensity();
        } else {
            reclaimDensity = MathUtil.normalizeBin(random.nextInt(GeneratedMapNameEncoder.NUM_BINS),
                                                   GeneratedMapNameEncoder.NUM_BINS);
        }

        if (customStyleOptions.getResourceDensity() != null) {
            resourceDensity = customStyleOptions.getResourceDensity();
        } else {
            resourceDensity = MathUtil.normalizeBin(random.nextInt(GeneratedMapNameEncoder.NUM_BINS),
                                                    GeneratedMapNameEncoder.NUM_BINS);
        }

        return new MapStyle.Custom(terrainStyle, biomeName, propStyle, resourceStyle, reclaimDensity,
                                   resourceDensity);
    }
}
