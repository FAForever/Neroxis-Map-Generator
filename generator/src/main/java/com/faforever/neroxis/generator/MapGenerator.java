package com.faforever.neroxis.generator;

import com.faforever.neroxis.cli.CLIUtils;
import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.OutputFolderMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.exporter.MapExporter;
import com.faforever.neroxis.exporter.SCMapExporter;
import com.faforever.neroxis.exporter.ScriptGenerator;
import com.faforever.neroxis.generator.cli.*;
import com.faforever.neroxis.generator.style.BasicStyleGenerator;
import com.faforever.neroxis.generator.style.CustomStyleGenerator;
import com.faforever.neroxis.generator.style.StyleGenerator;
import com.faforever.neroxis.map.DecalGroup;
import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.vector.Vector2;
import lombok.Getter;
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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static picocli.CommandLine.*;

@Getter
@Command(name = "generate", mixinStandardHelpOptions = true, description = "Generates a map from scratch", versionProvider = VersionProvider.class, usageHelpAutoWidth = true, sortOptions = false)
public class MapGenerator implements Callable<Integer> {
    public static final int NUM_BINS = 127;
    private static final String VERSION = new VersionProvider().getVersion()[0];

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
    @CommandLine.Option(names = "--num-to-generate", order = 2, defaultValue = "1", description = "Number of maps to create")
    private Integer numToGenerate;
    @CommandLine.ArgGroup(exclusive = false)
    private BasicOptions basicOptions = new BasicOptions();
    @CommandLine.ArgGroup()
    private GenerationOptions generationOptions = new GenerationOptions();
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

    @Option(names = "--preview-path", order = 10000, description = "Folder to save the map previews to")
    private void setPreviewFolder(Path previewFolder) throws IOException {
        CLIUtils.checkWritableDirectory(previewFolder, spec);
        this.previewFolder = previewFolder;
    }

    @Command(name = "styles", aliases = {
            "--styles"}, description = "Prints the styles available", versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
    private void printStyles() {
        System.out.println(Arrays.stream(MapStyle.values()).map(MapStyle::toString).collect(Collectors.joining("\n")));
    }

    @Command(name = "biomes", aliases = {
            "--biomes"}, description = "Prints the biomes available", versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
    private void printBiomes() {
        System.out.println(
                Arrays.stream(TextureGeneratorSupplier.values()).map(TextureGeneratorSupplier::toString).collect(Collectors.joining("\n")));
    }

    @Override
    public Integer call() throws Exception {
        Locale.setDefault(Locale.ROOT);

        populateGeneratorParametersAndName();

        FileUtil.deleteRecursiveIfExists(outputFolderMixin.getOutputPath().resolve(mapName));
        System.out.println(mapName);

        generate();
        save();

        System.out.printf("Saving map to %s%n", outputFolderMixin.getOutputPath().resolve(mapName).toAbsolutePath());

        Visibility visibility = Optional.ofNullable(generationOptions.getVisibilityOptions())
                                        .map(VisibilityOptions::getVisibility)
                                        .orElse(null);
        if (visibility == null) {
            System.out.printf("Seed: %d%n", basicOptions.getSeed());
            System.out.println(styleGenerator.getGeneratorParameters().toString());
            System.out.printf("Symmetry Settings: %s%n", styleGenerator.getSymmetrySettings());
            System.out.printf("Style: %s%n", generationOptions.getCasualOptions().getStyleOptions().getMapStyle());
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

    private void parseMapName(String mapName, GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
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

    private void parseOptions(byte[] optionBytes, GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
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
                generationOptions.getCasualOptions().getStyleOptions().setMapStyle(MapStyle.values()[optionBytes[4]]);
            } else if (optionBytes.length == 8) {
                generationOptions.getCasualOptions().getStyleOptions().setCustomStyleOptions(new CustomStyleOptions());
                generationOptions.getCasualOptions().getStyleOptions().getCustomStyleOptions().setTextureGenerator(
                        TextureGeneratorSupplier.values()[optionBytes[4]]);
                generationOptions.getCasualOptions().getStyleOptions().getCustomStyleOptions().setTerrainGenerator(
                        TerrainGeneratorSupplier.values()[optionBytes[5]]);
                generationOptions.getCasualOptions().getStyleOptions().getCustomStyleOptions().setResourceGenerator(
                        ResourceGeneratorSupplier.values()[optionBytes[6]]);
                generationOptions.getCasualOptions().getStyleOptions().getCustomStyleOptions().setPropGenerator(
                        PropGeneratorSupplier.values()[optionBytes[7]]);
            }
        }
    }

    private void randomizeOptions(GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        random = new Random(new Random(basicOptions.getSeed()).nextLong() ^ new Random(generationTime).nextLong());

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
            basicOptions.setSeed(new Random().nextLong());

            Visibility visibility = Optional.ofNullable(generationOptions.getVisibilityOptions())
                                            .map(VisibilityOptions::getVisibility)
                                            .orElse(null);

            generatorParametersBuilder.visibility(visibility);
        }
    }

    private void populateRequiredGeneratorParameters(GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        generatorParametersBuilder.mapSize(basicOptions.getMapSize());
        generatorParametersBuilder.numTeams(basicOptions.getNumTeams());
        generatorParametersBuilder.spawnCount(basicOptions.getSpawnCount());
    }

    @SuppressWarnings("unchecked")
    private void setStyleAndParameters(GeneratorParameters.GeneratorParametersBuilder generatorParametersBuilder) {
        CasualOptions casualOptions = generationOptions.getCasualOptions();
        if (casualOptions.getTerrainSymmetry() != null) {
            generatorParametersBuilder.terrainSymmetry(casualOptions.getTerrainSymmetry());
        }

        StyleOptions styleOptions = casualOptions.getStyleOptions();
        generatorParameters = generatorParametersBuilder.build();
        if (styleOptions.getMapStyle() == null) {
            if (styleOptions.getCustomStyleOptions() != null) {
                setCustomStyle();
            } else {
                WeightedOption<StyleGenerator>[] generatorOptions = Arrays.stream(MapStyle.values()).map(mapStyle -> {
                    StyleGenerator styleGenerator = mapStyle.getGeneratorSupplier().get();
                    return new WeightedOption<>(styleGenerator, mapStyle.getWeight());
                }).toArray(WeightedOption[]::new);
                Map<Class<? extends StyleGenerator>, MapStyle> styleMap = Arrays.stream(MapStyle.values())
                        .collect(Collectors.toMap(
                                MapStyle::getGeneratorClass,
                                Function.identity()));
                WeightedOptionsWithFallback<StyleGenerator> styleGeneratorOptions = WeightedOptionsWithFallback.of(
                        new BasicStyleGenerator(), generatorOptions);
                styleGenerator = styleGeneratorOptions.select(random,
                        styleGenerator -> styleGenerator.getParameterConstraints()
                                .matches(
                                        generatorParameters));
                styleOptions.setMapStyle(styleMap.get(styleGenerator.getClass()));
            }
        } else {
            styleGenerator = styleOptions.getMapStyle().getGeneratorSupplier().get();;
        }
    }

    private void setCustomStyle() {
        TextureGeneratorSupplier textureGenerator = TextureGeneratorSupplier.values()[random.nextInt(TextureGeneratorSupplier.values().length)];
        TerrainGeneratorSupplier terrainGenerator = TerrainGeneratorSupplier.values()[random.nextInt(TerrainGeneratorSupplier.values().length)];
        ResourceGeneratorSupplier resourceGenerator = ResourceGeneratorSupplier.values()[random.nextInt(ResourceGeneratorSupplier.values().length)];
        PropGeneratorSupplier propGenerator = PropGeneratorSupplier.values()[random.nextInt(PropGeneratorSupplier.values().length)];

        CustomStyleOptions customStyleOptions = generationOptions.getCasualOptions().getStyleOptions().getCustomStyleOptions();
        if (customStyleOptions.getTextureGenerator() == null) {
            customStyleOptions.setTextureGenerator(textureGenerator);
        }
        if (customStyleOptions.getTerrainGenerator() == null) {
            customStyleOptions.setTerrainGenerator(terrainGenerator);
        }
        if (customStyleOptions.getResourceGenerator() == null) {
            customStyleOptions.setResourceGenerator(resourceGenerator);
        }
        if (customStyleOptions.getPropGenerator() == null) {
            customStyleOptions.setPropGenerator(propGenerator);
        }

        CustomStyleGenerator customStyleGenerator = new CustomStyleGenerator();
        customStyleGenerator.setTerrainGeneratorSupplier(customStyleOptions.getTerrainGenerator());
        customStyleGenerator.setTextureGeneratorSupplier(customStyleOptions.getTextureGenerator());
        customStyleGenerator.setResourceGeneratorSupplier(customStyleOptions.getResourceGenerator());
        customStyleGenerator.setPropGeneratorSupplier(customStyleOptions.getPropGenerator());
        styleGenerator = customStyleGenerator;
    }

    private void encodeMapName() {
        CommandLine.ParseResult parseResult = spec.commandLine().getParseResult();
        if (this.mapName == null) {
            Visibility visibility = generatorParameters.visibility();

            String mapNameFormat = "neroxis_map_generator_%s_%s_%s";
            ByteBuffer seedBuffer = ByteBuffer.allocate(8);
            seedBuffer.putLong(basicOptions.getSeed());
            String seedString = GeneratedMapNameEncoder.encode(seedBuffer.array());
            byte[] optionArray;
            StyleOptions styleOptions = generationOptions.getCasualOptions().getStyleOptions();
            if (styleOptions.getCustomStyleOptions() != null)
            {
                optionArray = new byte[]{(byte) generatorParameters.spawnCount(),
                                         (byte) (generatorParameters.mapSize() / 64),
                                         (byte) generatorParameters.numTeams(),
                                         (byte) generatorParameters.terrainSymmetry().ordinal(),
                                         (byte) styleOptions.getCustomStyleOptions().getTextureGenerator().ordinal(),
                                         (byte) styleOptions.getCustomStyleOptions().getTerrainGenerator().ordinal(),
                                         (byte) styleOptions.getCustomStyleOptions().getResourceGenerator().ordinal(),
                                         (byte) styleOptions.getCustomStyleOptions().getPropGenerator().ordinal()};
            } else if (generationOptions.getVisibilityOptions() != null) {
                optionArray = new byte[]{(byte) generatorParameters.spawnCount(),
                                         (byte) (generatorParameters.mapSize() / 64),
                                         (byte) generatorParameters.numTeams(),
                                         (byte) visibility.ordinal()};
            } else if (parseResult.hasMatchedOption("--style")) {
                optionArray = new byte[]{(byte) generatorParameters.spawnCount(),
                                         (byte) (generatorParameters.mapSize() / 64),
                                         (byte) generatorParameters.numTeams(),
                                         (byte) generatorParameters.terrainSymmetry().ordinal(),
                                         (byte) styleOptions
                                                                 .getMapStyle()
                                                                 .ordinal()};
            } else {
                optionArray = new byte[]{(byte) generatorParameters.spawnCount(),
                                         (byte) (generatorParameters.mapSize() / 64),
                                         (byte) generatorParameters.numTeams(),
                                         (byte) generatorParameters.terrainSymmetry().ordinal()};
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

        if (DebugUtil.DEBUG) {
            System.out.printf("Style selection done: %d ms\n", System.currentTimeMillis() - sTime);
        }

        map = styleGenerator.generate(generatorParameters, random.nextLong());
        Visibility visibility = styleGenerator.getGeneratorParameters().visibility();

        StringBuilder descriptionBuilder = new StringBuilder();
        if (visibility == null) {
            descriptionBuilder.append("Seed: ").append(basicOptions.getSeed()).append("\n");
            descriptionBuilder.append(styleGenerator.getGeneratorParameters().toString()).append("\n");
            descriptionBuilder.append("Style: ")
                              .append(generationOptions.getCasualOptions().getStyleOptions().getMapStyle())
                              .append("\n");
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

        map.addBlank(new Marker(mapName, new Vector2(0, 0)));
        map.addDecalGroup(new DecalGroup(mapName, List.of()));
        map.setName(mapName);
        map.setFolderName(mapName);
        map.setFilePrefix(mapName);

        ScriptGenerator.generateScript(map);

        System.out.printf("Map generation done: %d ms\n", System.currentTimeMillis() - startTime);
    }

    private void save() {
        try {
            long startTime = System.currentTimeMillis();
            Path outputPath = outputFolderMixin.getOutputPath();
            Visibility visibility = generatorParameters.visibility();
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
                                   """.formatted(basicOptions.getSeed(),
                                                 generatorParameters.toString(),
                                                 generationOptions.getCasualOptions().getStyleOptions().getMapStyle(),
                                                 styleGenerator.generatorsToString());
            out.write(summaryString.getBytes());
            out.flush();
            out.close();
        }
    }
}
