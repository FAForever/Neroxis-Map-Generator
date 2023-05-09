package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.cli.CLIUtils;
import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.OutputFolderMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.exporter.MapExporter;
import com.faforever.neroxis.exporter.SCMapExporter;
import com.faforever.neroxis.exporter.ScriptGenerator;
import com.faforever.neroxis.generator.cli.ParameterOptions;
import com.faforever.neroxis.generator.cli.TuningOptions;
import com.faforever.neroxis.generator.cli.VisibilityOptions;
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
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.MathUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.vector.Vector2;
import lombok.Getter;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Getter
@Command(name = "generate", mixinStandardHelpOptions = true, description = "Generates a map from scratch", versionProvider = VersionProvider.class, usageHelpAutoWidth = true, sortOptions = false)
public class MapGenerator implements Callable<Integer> {
    public static final int NUM_BINS = 127;
    private static final String VERSION = new VersionProvider().getVersion()[0];
    private final List<StyleGenerator> mapStyles = List.of(new BigIslandsStyleGenerator(), new CenterLakeStyleGenerator(), new BasicStyleGenerator(), new DropPlateauStyleGenerator(), new LandBridgeStyleGenerator(), new LittleMountainStyleGenerator(), new MountainRangeStyleGenerator(), new OneIslandStyleGenerator(), new SmallIslandsStyleGenerator(), new ValleyStyleGenerator(), new HighReclaimStyleGenerator(), new LowMexStyleGenerator(), new FloodedStyleGenerator(), new TestStyleGenerator());
    private final List<StyleGenerator> productionStyles = mapStyles.stream()
                                                                   .filter(styleGenerator -> !(styleGenerator instanceof TestStyleGenerator))
                                                                   .toList();
    @Spec
    private CommandLine.Model.CommandSpec spec;
    // Set during generation
    private SCMap map;
    private long generationTime;
    private Random random;
    private GeneratorParameters generatorParameters;
    private StyleGenerator styleGenerator;
    @CommandLine.Option(names = "--map-name", order = 1, description = "Name of map to recreate. Must be of the form neroxis_map_generator_version_seed_options, if present other parameter options will be ignored")
    private String mapName;
    @CommandLine.Option(names = "--seed", order = 2, description = "Seed for the generated map")
    private long seed = new Random().nextLong();
    @CommandLine.Option(names = "--spawn-count", order = 4, defaultValue = "6", description = "Spawn count for the generated map", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private Integer spawnCount;
    @CommandLine.Option(names = "--num-teams", order = 5, defaultValue = "2", description = "Number of teams for the generated map (0 is no teams asymmetric)", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private Integer numTeams;
    @CommandLine.Option(names = "--num-to-generate", order = 6, defaultValue = "1", description = "Number of maps to create")
    private Integer numToGenerate;
    private Integer mapSize;
    @CommandLine.ArgGroup(order = 2)
    private TuningOptions tuningOptions = new TuningOptions();
    @CommandLine.Mixin
    private OutputFolderMixin outputFolderMixin;
    @CommandLine.Mixin
    private DebugMixin debugMixin;
    private Path previewFolder;

    public static void main(String[] args) {
        DebugUtil.timedRun("Execution", () -> {
            CommandLine numToGenerateParser = new CommandLine(new MapGenerator());
            numToGenerateParser.setAbbreviatedOptionsAllowed(true);
            int numToGenerate = numToGenerateParser.parseArgs(args).matchedOptionValue("num-to-generate", 1);

            for (int i = 0; i < numToGenerate; i++) {
                CommandLine commandLine = new CommandLine(new MapGenerator());
                commandLine.setAbbreviatedOptionsAllowed(true);
                commandLine.execute(args);
            }
            Pipeline.shutdown();
        });
    }

    @CommandLine.Option(names = "--map-size", order = 3, defaultValue = "512", description = "Generated map size, can be specified in oGrids (e.g 512) or km (e.g 10km)", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    public void setMapSize(String mapSizeString) {
        this.mapSize = CLIUtils.convertMapSizeString(mapSizeString, CLIUtils.MapSizeStrictness.DISCRETE_64, spec);
    }

    public void setMapSize(int mapSize) {
        this.mapSize = mapSize;
    }

    @Option(names = "--preview-path", order = 100, description = "Folder to save the map previews to")
    private void setPreviewFolder(Path previewFolder) throws IOException {
        CLIUtils.checkWritableDirectory(previewFolder, spec);
        this.previewFolder = previewFolder;
    }

    @Command(name = "styles", aliases = {
            "--styles"}, description = "Prints the styles available", versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
    private void printStyles() {
        System.out.println(Arrays.stream(MapStyle.values())
                                 .filter(MapStyle::isProduction)
                                 .map(MapStyle::toString)
                                 .collect(Collectors.joining("\n")));
    }

    @Override
    public Integer call() throws Exception {
        Locale.setDefault(Locale.ROOT);

        populateGeneratorParametersAndName();

        FileUtil.deleteRecursiveIfExists(outputFolderMixin.getOutputPath().resolve(mapName));
        System.out.println(mapName);

        generate();
        save();

        System.out.printf("Saving map to %s%n", outputFolderMixin.getOutputPath()
                                                                 .resolve(mapName)
                                                                 .toAbsolutePath());

        Visibility visibility = Optional.ofNullable(tuningOptions.getVisibilityOptions())
                                        .map(VisibilityOptions::getVisibility)
                                        .orElse(null);
        if (visibility == null) {
            System.out.printf("Seed: %d%n", seed);
            System.out.println(styleGenerator.getGeneratorParameters().toString());
            System.out.printf("Symmetry Settings: %s%n", styleGenerator.getSymmetrySettings());
            System.out.printf("Style: %s%n", tuningOptions.getMapStyle());
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
        } else {
            checkParameters();
            setVisibility(generatorParametersBuilder);
            populateRequiredGeneratorParameters(generatorParametersBuilder);

            randomizeOptions(generatorParametersBuilder);
        }

        setStyleAndParameters(generatorParametersBuilder);
        encodeMapName();
    }

    private void setStyleAndParameters(GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        if (tuningOptions.getMapStyle() == null) {
            overwriteOptionalGeneratorParametersFromOptions(generatorParametersBuilder);
            generatorParameters = generatorParametersBuilder.build();
            List<StyleGenerator> productionStyles = Arrays.stream(MapStyle.values())
                                                          .filter(MapStyle::isProduction)
                                                          .map(MapStyle::getGeneratorClass)
                                                          .map(clazz -> {
                                                              try {
                                                                  return clazz.getConstructor().newInstance();
                                                              } catch (InstantiationException | IllegalAccessException |
                                                                       InvocationTargetException |
                                                                       NoSuchMethodException e) {
                                                                  throw new RuntimeException(e);
                                                              }
                                                          })
                                                          .collect(Collectors.toList());
            Map<Class<? extends StyleGenerator>, MapStyle> styleMap = Arrays.stream(MapStyle.values())
                                                                            .collect(Collectors.toMap(MapStyle::getGeneratorClass, style -> style));
            styleGenerator = StyleGenerator.selectRandomMatchingGenerator(random, productionStyles, generatorParameters, new BasicStyleGenerator());
            tuningOptions.setMapStyle(styleMap.get(styleGenerator.getClass()));
        } else {
            try {
                styleGenerator = tuningOptions.getMapStyle().getGeneratorClass().getConstructor().newInstance();
                generatorParameters = styleGenerator.getParameterConstraints()
                                                    .initParameters(random, generatorParametersBuilder);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void populateRequiredGeneratorParameters(GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        generatorParametersBuilder.mapSize(mapSize);
        generatorParametersBuilder.numTeams(numTeams);
        generatorParametersBuilder.spawnCount(spawnCount);
    }

    private void overwriteOptionalGeneratorParametersFromOptions(GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        ParameterOptions parameterOptions = tuningOptions.getParameterOptions();
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

    private void setVisibility(GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        if (tuningOptions.getVisibilityOptions() != null) {
            generationTime = Instant.now().getEpochSecond();
            seed = new Random().nextLong();

            Visibility visibility = Optional.ofNullable(tuningOptions.getVisibilityOptions())
                                            .map(VisibilityOptions::getVisibility)
                                            .orElse(null);

            generatorParametersBuilder.visibility(visibility);
        }
    }

    private void checkParameters() {
        if (numTeams != 0 && spawnCount % numTeams != 0) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format("Spawn Count `%d` not a multiple of Num Teams `%d`", spawnCount, numTeams));
        }

        ParameterOptions parameterOptions = tuningOptions.getParameterOptions();

        if (parameterOptions != null &&
            parameterOptions.getTerrainSymmetry() != null &&
            numTeams % parameterOptions.getTerrainSymmetry()
                                       .getNumSymPoints() != 0) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format("Terrain symmetry `%s` not compatible with Num Teams `%d`", parameterOptions.getTerrainSymmetry(), numTeams));
        }
    }

    private Symmetry getValidTerrainSymmetry() {
        List<Symmetry> terrainSymmetries = switch (spawnCount) {
            case 2, 4, 8 ->
                    new ArrayList<>(Arrays.asList(Symmetry.POINT2, Symmetry.POINT4, Symmetry.POINT6, Symmetry.POINT8, Symmetry.QUAD, Symmetry.DIAG));
            default -> new ArrayList<>(Arrays.asList(Symmetry.values()));
        };
        terrainSymmetries.remove(Symmetry.X);
        terrainSymmetries.remove(Symmetry.Z);
        if (numTeams > 1) {
            terrainSymmetries.remove(Symmetry.NONE);
            terrainSymmetries.removeIf(symmetry -> symmetry.getNumSymPoints() % numTeams != 0 ||
                                                   symmetry.getNumSymPoints() > spawnCount * 4);
        } else {
            terrainSymmetries.clear();
            terrainSymmetries.add(Symmetry.NONE);
        }
        if (numTeams == 2 && random.nextFloat() < .75f) {
            terrainSymmetries.removeIf(symmetry -> !symmetry.isPerfectSymmetry());
        }
        return terrainSymmetries.get(random.nextInt(terrainSymmetries.size()));
    }

    private void parseMapName(String mapName, GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        this.mapName = mapName;

        String[] nameArgs = verifyMapName(mapName);

        String seedString = nameArgs[4];
        seed = ByteBuffer.wrap(GeneratedMapNameEncoder.decode(seedString)).getLong();

        if (nameArgs.length >= 7) {
            String timeString = nameArgs[6];
            generationTime = ByteBuffer.wrap(GeneratedMapNameEncoder.decode(timeString)).getLong();
        }

        if (nameArgs.length >= 6) {
            String optionString = nameArgs[5];
            parseOptions(GeneratedMapNameEncoder.decode(optionString), generatorParametersBuilder);
        }

        populateRequiredGeneratorParameters(generatorParametersBuilder);
    }

    private String[] verifyMapName(String mapName) {
        verifyMapNamePrefix(mapName);
        String[] nameArgs = mapName.split("_");
        verifyNameArgs(mapName, nameArgs);
        return nameArgs;
    }

    private void verifyNameArgs(String mapName, String[] nameArgs) {
        if (nameArgs.length < 4) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format("Map name `%s` does not specify a version", mapName));
        }

        String version = nameArgs[3];
        if (!VERSION.equals(version)) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format("Version for `%s` does not match this generator version", mapName));
        }

        if (nameArgs.length < 5) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format("Map name `%s` does not specify a seed", mapName));
        }
    }

    private void verifyMapNamePrefix(String mapName) {
        if (!mapName.startsWith("neroxis_map_generator")) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format("Map name `%s` is not a generated map", mapName));
        }
    }

    private void parseOptions(byte[] optionBytes, GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        if (optionBytes.length > 0) {
            spawnCount = (int) optionBytes[0];
            if (spawnCount <= 16) {
                generatorParametersBuilder.spawnCount(spawnCount);
            }
        }
        if (optionBytes.length > 1) {
            mapSize = optionBytes[1] * 64;
            generatorParametersBuilder.mapSize(mapSize);
        }

        if (optionBytes.length > 2) {
            numTeams = (int) optionBytes[2];
            generatorParametersBuilder.numTeams(numTeams);
        }

        randomizeOptions(generatorParametersBuilder);

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
                tuningOptions.setMapStyle(MapStyle.values()[optionBytes[3]]);
            } else {
                Visibility visibility = Visibility.values()[optionBytes[3]];
                generatorParametersBuilder.visibility(visibility);
            }
        }
    }

    private void randomizeOptions(GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        random = new Random(new Random(seed).nextLong() ^ new Random(generationTime).nextLong());

        generatorParametersBuilder.landDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.plateauDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.mountainDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.rampDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.reclaimDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.mexDensity(MathUtil.discretePercentage(random.nextFloat(), NUM_BINS));
        generatorParametersBuilder.biome(Biomes.loadBiome(Biomes.BIOMES_LIST.get(random.nextInt(Biomes.BIOMES_LIST.size()))));
        generatorParametersBuilder.terrainSymmetry(getValidTerrainSymmetry());
    }

    private void encodeMapName() {
        CommandLine.ParseResult parseResult = spec.commandLine().getParseResult();
        if (this.mapName == null) {
            Visibility visibility = generatorParameters.visibility();

            String mapNameFormat = "neroxis_map_generator_%s_%s_%s";
            ByteBuffer seedBuffer = ByteBuffer.allocate(8);
            seedBuffer.putLong(seed);
            String seedString = GeneratedMapNameEncoder.encode(seedBuffer.array());
            byte[] optionArray;
            if (tuningOptions.getParameterOptions() != null) {
                optionArray = new byte[]{(byte) generatorParameters.spawnCount(),
                                         (byte) (generatorParameters.mapSize() / 64),
                                         (byte) generatorParameters.numTeams(),
                                         (byte) Biomes.BIOMES_LIST.indexOf(generatorParameters.biome().name()),
                                         (byte) MathUtil.binPercentage(generatorParameters.landDensity(), NUM_BINS),
                                         (byte) MathUtil.binPercentage(generatorParameters.plateauDensity(), NUM_BINS),
                                         (byte) MathUtil.binPercentage(generatorParameters.mountainDensity(), NUM_BINS),
                                         (byte) MathUtil.binPercentage(generatorParameters.rampDensity(), NUM_BINS),
                                         (byte) MathUtil.binPercentage(generatorParameters.reclaimDensity(), NUM_BINS),
                                         (byte) MathUtil.binPercentage(generatorParameters.mexDensity(), NUM_BINS),
                                         (byte) generatorParameters.terrainSymmetry()
                                                                   .ordinal()};
            } else if (tuningOptions.getVisibilityOptions() != null) {
                optionArray = new byte[]{(byte) generatorParameters.spawnCount(),
                                         (byte) (generatorParameters.mapSize() / 64),
                                         (byte) generatorParameters.numTeams(),
                                         (byte) visibility.ordinal()};
            } else if (parseResult.hasMatchedOption("--style")) {
                optionArray = new byte[]{(byte) generatorParameters.spawnCount(),
                                         (byte) (generatorParameters.mapSize() / 64),
                                         (byte) generatorParameters.numTeams(),
                                         (byte) tuningOptions.getMapStyle().ordinal()};
            } else {
                optionArray = new byte[]{(byte) generatorParameters.spawnCount(),
                                         (byte) (generatorParameters.mapSize() / 64),
                                         (byte) generatorParameters.numTeams()};
            }
            String optionString = GeneratedMapNameEncoder.encode(optionArray);
            if (visibility != null) {
                String timeString = GeneratedMapNameEncoder.encode(ByteBuffer.allocate(8)
                                                                             .putLong(generationTime)
                                                                             .array());
                optionString += "_" + timeString;
            }
            mapName = String.format(mapNameFormat, VERSION, seedString, optionString).toLowerCase();
        }
    }

    private void save() {
        try {
            long startTime = System.currentTimeMillis();
            Path outputPath = outputFolderMixin.getOutputPath();
            Visibility visibility = generatorParameters.visibility();
            SCMapExporter.exportPBR(outputPath.resolve(mapName), map);
            MapExporter.exportMap(outputPath, map, visibility == null);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

            if (visibility == null && DebugUtil.DEBUG) {
                startTime = System.currentTimeMillis();
                Files.createDirectory(outputPath.resolve(mapName).resolve("debug"));
                SCMapExporter.exportSCMapString(outputPath, mapName, map);
                Pipeline.toFile(outputPath.resolve(mapName).resolve("debug").resolve("pipelineMaskHashes.txt"));
                toFile(outputPath.resolve(mapName).resolve("debug").resolve("generatorParams.txt"));
                System.out.printf("Debug export done: %d ms\n", System.currentTimeMillis() - startTime);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    private void generate() {
        long startTime = System.currentTimeMillis();
        long sTime = System.currentTimeMillis();

        if (DebugUtil.DEBUG) {
            System.out.printf("Style selection done: %d ms\n", System.currentTimeMillis() - sTime);
        }

        map = styleGenerator.generate(generatorParameters, random.nextLong());
        Visibility visibility = styleGenerator.getGeneratorParameters().visibility();

        StringBuilder descriptionBuilder = new StringBuilder();
        if (visibility == null) {
            descriptionBuilder.append("Seed: ").append(seed).append("\n");
            descriptionBuilder.append(styleGenerator.getGeneratorParameters().toString()).append("\n");
            descriptionBuilder.append("Style: ").append(tuningOptions.getMapStyle()).append("\n");
            descriptionBuilder.append(styleGenerator.generatorsToString()).append("\n");
        } else {
            descriptionBuilder.append(String.format("Map originally generated at %s UTC\n", DateTimeFormatter.ofPattern("HH:mm:ss dd MMM uuuu")
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

        map.addBlank(new Marker(mapName, new Vector2(0, 0)));
        map.addDecalGroup(new DecalGroup(mapName, new int[0]));
        map.setName(mapName);
        map.setFolderName(mapName);
        map.setFilePrefix(mapName);

        map.getBiome()
           .terrainMaterials()
           .getTexturePaths()[8] = Path.of("/maps", map.getFolderName(), "env", "texture", "pbr.dds")
                                       .toString()
                                       .replace("\\", "/");
        map.getBiome().terrainMaterials().getTextureScales()[8] = 0;

        ScriptGenerator.generateScript(map);

        System.out.printf("Map generation done: %d ms\n", System.currentTimeMillis() - startTime);
    }

    private void toFile(Path path) throws IOException {
        Files.deleteIfExists(path);
        File outFile = path.toFile();
        boolean status = outFile.createNewFile();
        if (status) {
            FileOutputStream out = new FileOutputStream(outFile);
            String summaryString = "Seed: "
                                   + seed
                                   + "\n"
                                   + generatorParameters.toString()
                                   + "\nStyle: "
                                   + tuningOptions.getMapStyle()
                                   + "\n"
                                   + styleGenerator.generatorsToString();
            out.write(summaryString.getBytes());
            out.flush();
            out.close();
        }
    }
}
