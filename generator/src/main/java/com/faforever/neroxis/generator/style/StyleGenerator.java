package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.Visibility;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.decal.BasicDecalGenerator;
import com.faforever.neroxis.generator.decal.DecalGenerator;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.resource.BasicResourceGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.BasicLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.SpawnFirstTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.texture.BrimstoneTextureGenerator;
import com.faforever.neroxis.generator.texture.CrystallineTextureGenerator;
import com.faforever.neroxis.generator.texture.DesertTextureGenerator;
import com.faforever.neroxis.generator.texture.EarlyAutumnTextureGenerator;
import com.faforever.neroxis.generator.texture.FrithenTextureGenerator;
import com.faforever.neroxis.generator.texture.MarsTextureGenerator;
import com.faforever.neroxis.generator.texture.PrayerTextureGenerator;
import com.faforever.neroxis.generator.texture.StonesTextureGenerator;
import com.faforever.neroxis.generator.texture.SunsetTextureGenerator;
import com.faforever.neroxis.generator.texture.TextureGenerator;
import com.faforever.neroxis.generator.texture.WindingRiverTextureGenerator;
import com.faforever.neroxis.generator.texture.WonderTextureGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.generator.util.SpawnPlacementException;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.SpawnPlacer;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.SymmetrySelector;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

@NullUnmarked
public abstract class StyleGenerator implements HasParameterConstraints {
    private static final ExecutorService PLACEMENT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private @Nullable List<Pipeline.Entry> terrainPipelineEntries;
    private @Nullable List<Pipeline.Entry> placementPipelineEntries;

    private TerrainGenerator terrainGenerator;
    private TextureGenerator textureGenerator;
    private ResourceGenerator resourceGenerator;
    private PropGenerator propGenerator;
    private DecalGenerator decalGenerator;
    private SCMap map;
    private RandomGenerator.SplittableGenerator random;

    @Setter
    private boolean debug;
    @Setter
    private boolean visualize;

    @Getter
    private GeneratorParameters generatorParameters;
    @Getter
    @Setter
    private SymmetrySettings symmetrySettings;

    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicLastTerrainGenerator());
    }

    protected WeightedOptionsWithFallback<TextureGenerator> getTextureGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BrimstoneTextureGenerator(),
                                              new WeightedOption<>(new BrimstoneTextureGenerator(), 1f),
                                              new WeightedOption<>(new DesertTextureGenerator(), 1f),
                                              new WeightedOption<>(new EarlyAutumnTextureGenerator(), 1f),
                                              new WeightedOption<>(new FrithenTextureGenerator(), 1f),
                                              new WeightedOption<>(new MarsTextureGenerator(), 1f),
                                              new WeightedOption<>(new PrayerTextureGenerator(), 1f),
                                              new WeightedOption<>(new StonesTextureGenerator(), 1f),
                                              new WeightedOption<>(new SunsetTextureGenerator(), 1f),
                                              new WeightedOption<>(new WindingRiverTextureGenerator(), 1f),
                                              new WeightedOption<>(new WonderTextureGenerator(), 1f),
                                              new WeightedOption<>(new CrystallineTextureGenerator(), 1f));
    }

    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicResourceGenerator());
    }

    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicPropGenerator());
    }

    protected WeightedOptionsWithFallback<DecalGenerator> getDecalGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicDecalGenerator());
    }

    public SCMap generate(GeneratorParameters generatorParameters) {
        initialize(generatorParameters);

        while (map.getSpawnCount() != generatorParameters.spawnCount()) {
            try {
                generateTerrain();
            } catch (CompletionException exception) {
                switch (exception.getCause()) {
                    case SpawnPlacementException _ ->
                            System.out.println("Unable to place all spawns trying new generation");
                    case Throwable _ -> throw exception;
                }
            }
        }

        generatePlacements();

        return map;
    }

    private void generateTerrain() {
        if (terrainGenerator instanceof SpawnFirstTerrainGenerator) {
            SpawnPlacer spawnPlacer = new SpawnPlacer(map, random.split());
            DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeSpawns",
                               () -> spawnPlacer.placeSpawns(generatorParameters.spawnCount(), getSpawnSeparation(),
                                                             getTeamSeparation(), symmetrySettings));
        }

        terrainPipelineEntries = Pipeline.run(pipeline -> {
            pipeline.setDebug(debug);
            pipeline.setVisualize(visualize);
        }, () -> {
            terrainGenerator.initialize(map, random.split(), this.generatorParameters, symmetrySettings);
            terrainGenerator.setupPipeline();
        });

        CompletableFuture<Void> heightMapFuture = CompletableFuture.runAsync(terrainGenerator::setHeightmapImage,
                                                                             PLACEMENT_EXECUTOR);

        CompletableFuture<Void> spawnFuture = CompletableFuture.runAsync(terrainGenerator::placeSpawns,
                                                                         PLACEMENT_EXECUTOR);

        CompletableFuture.allOf(heightMapFuture, spawnFuture).join();
    }

    private float getSpawnSeparation() {
        if (generatorParameters.numTeams() < 2) {
            return (float) generatorParameters.mapSize() / generatorParameters.spawnCount() * 1.5f;
        } else if (generatorParameters.numTeams() == 2) {
            return random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16f;
        } else {
            if (generatorParameters.numTeams() < 8) {
                return random.nextInt(map.getSize() / 2 / generatorParameters.numTeams() - map.getSize() / 16) +
                       map.getSize() / 16f;
            } else {
                return 0;
            }
        }
    }

    private int getTeamSeparation() {
        if (generatorParameters.numTeams() < 2) {
            return 0;
        } else if (generatorParameters.numTeams() == 2) {
            return map.getSize() / 2;
        } else {
            return StrictMath.min(map.getSize() / generatorParameters.numTeams(), 256);
        }
    }

    private void generatePlacements() {
        placementPipelineEntries = Pipeline.run(pipeline -> {
            pipeline.setDebug(debug);
            pipeline.setVisualize(visualize);
        }, () -> {
            textureGenerator.initialize(map, random.split(), this.generatorParameters,
                                        new SymmetrySettings(Symmetry.NONE), terrainGenerator);
            resourceGenerator.initialize(map, random.split(), this.generatorParameters, symmetrySettings,
                                         terrainGenerator);
            propGenerator.initialize(map, random.split(), this.generatorParameters, symmetrySettings, terrainGenerator);
            decalGenerator.initialize(map, random.split(), this.generatorParameters, symmetrySettings,
                                      terrainGenerator);

            resourceGenerator.setupPipeline();
            textureGenerator.setupPipeline();
            propGenerator.setupPipeline();
            decalGenerator.setupPipeline();
        });

        CompletableFuture<Void> textureFuture = CompletableFuture.runAsync(textureGenerator::setTextures,
                                                                           PLACEMENT_EXECUTOR);
        CompletableFuture<Void> normalFuture = CompletableFuture.runAsync(textureGenerator::setCompressedDecals,
                                                                          PLACEMENT_EXECUTOR);

        CompletableFuture<Void> resourcesFuture = CompletableFuture.runAsync(resourceGenerator::placeResources,
                                                                             PLACEMENT_EXECUTOR);
        CompletableFuture<Void> decalsFuture = CompletableFuture.runAsync(decalGenerator::placeDecals,
                                                                          PLACEMENT_EXECUTOR);
        CompletableFuture<Void> propsFuture = resourcesFuture.thenRunAsync(propGenerator::placeProps,
                                                                           PLACEMENT_EXECUTOR);
        CompletableFuture<Void> unitsFuture = resourcesFuture.thenRunAsync(propGenerator::placeUnits,
                                                                           PLACEMENT_EXECUTOR);

        CompletableFuture<Void> previewFuture = propsFuture.thenRunAsync(textureGenerator::generatePreview,
                                                                         PLACEMENT_EXECUTOR);

        CompletableFuture.allOf(textureFuture, previewFuture, resourcesFuture, decalsFuture, propsFuture, unitsFuture,
                                normalFuture).thenRunAsync(this::setHeights, PLACEMENT_EXECUTOR).join();
    }

    protected void initialize(GeneratorParameters generatorParameters) {
        this.random = generatorParameters.createRandom();
        this.generatorParameters = generatorParameters;
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "selectGenerators", () -> {
            Predicate<HasParameterConstraints> constraintsMatchPredicate = hasConstraints -> hasConstraints.getParameterConstraints()
                                                                                                           .matches(
                                                                                                                   generatorParameters);
            terrainGenerator = getTerrainGeneratorOptions().select(this.random, constraintsMatchPredicate);
            textureGenerator = getTextureGeneratorOptions().select(this.random, constraintsMatchPredicate);
            resourceGenerator = getResourceGeneratorOptions().select(this.random, constraintsMatchPredicate);
            propGenerator = getPropGeneratorOptions().select(this.random, constraintsMatchPredicate);
            decalGenerator = getDecalGeneratorOptions().select(this.random, constraintsMatchPredicate);
        });

        symmetrySettings = switch (generatorParameters.mode()) {
            case GeneratorParameters.Casual(Symmetry terrainSymmetry, _) when terrainSymmetry != null ->
                    SymmetrySelector.getSymmetrySettingsFromTerrainSymmetry(
                            random, terrainSymmetry, generatorParameters.spawnCount(),
                            generatorParameters.numTeams());
            default -> SymmetrySelector.getSymmetrySettings(random,
                                                            generatorParameters.spawnCount(),
                                                            generatorParameters.numTeams());
        };

        map = new SCMap(generatorParameters.mapSize(), textureGenerator.loadBiome());
        if (generatorParameters.mode() instanceof GeneratorParameters.Competitive(_, Visibility visibility)) {
            map.setUnexplored(visibility == Visibility.UNEXPLORED);
            map.setGeneratePreview(visibility != Visibility.BLIND && !map.isUnexplored());
        }
    }

    protected void setHeights() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "setPlacements", () -> map.setHeights());
    }

    public String generatorsToString() {
        if (generatorParameters.isCasual()) {
            return """
                   Symmetry Settings: %s
                   TerrainGenerator: %s
                   TextureGenerator: %s
                   ResourceGenerator: %s
                   PropGenerator: %s
                   DecalGenerator: %s
                   Resource Density: %s
                   Reclaim Density: %s
                   """.formatted(symmetrySettings, terrainGenerator.getClass().getSimpleName(),
                                 textureGenerator.getClass().getSimpleName(),
                                 resourceGenerator.getClass().getSimpleName(), propGenerator.getClass().getSimpleName(),
                                 decalGenerator.getClass().getSimpleName(), resourceGenerator.getResourceDensity(),
                                 propGenerator.getReclaimDensity());
        } else {
            return "";
        }
    }

    public final void writePipelines(OutputStream out) throws IOException {
        if (terrainPipelineEntries == null || placementPipelineEntries == null) {
            throw new IllegalStateException("Generate has not been run");
        }
        try {
            for (Pipeline.Entry entry : terrainPipelineEntries) {
                out.write(entry.getResult().toHash().getBytes(StandardCharsets.UTF_8));
                out.write("\n".getBytes(StandardCharsets.UTF_8));
            }
            for (Pipeline.Entry entry : placementPipelineEntries) {
                out.write(entry.getResult().toHash().getBytes(StandardCharsets.UTF_8));
                out.write("\n".getBytes(StandardCharsets.UTF_8));
            }
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException(exception);
        }
        out.flush();
    }

    public final List<String> getPipelineHashes() {
        List<String> hashes = new ArrayList<>();
        try {
            for (Pipeline.Entry entry : terrainPipelineEntries) {
                hashes.add(entry.getResult().toHash());
            }
            for (Pipeline.Entry entry : placementPipelineEntries) {
                hashes.add(entry.getResult().toHash());
            }
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException(exception);
        }
        return List.copyOf(hashes);
    }
}
