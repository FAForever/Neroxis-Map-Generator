package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.SpawnFirstTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.texture.BrimstoneTextureGenerator;
import com.faforever.neroxis.generator.texture.CrystallineTextureGenerator;
import com.faforever.neroxis.generator.texture.DesertTextureGenerator;
import com.faforever.neroxis.generator.texture.EarlyAutumnTextureGenerator;
import com.faforever.neroxis.generator.texture.FrithenTextureGenerator;
import com.faforever.neroxis.generator.texture.MarsTextureGenerator;
import com.faforever.neroxis.generator.texture.MoonlightTextureGenerator;
import com.faforever.neroxis.generator.texture.PrayerTextureGenerator;
import com.faforever.neroxis.generator.texture.StonesTextureGenerator;
import com.faforever.neroxis.generator.texture.SunsetTextureGenerator;
import com.faforever.neroxis.generator.texture.SyrtisTextureGenerator;
import com.faforever.neroxis.generator.texture.TextureGenerator;
import com.faforever.neroxis.generator.texture.WindingRiverTextureGenerator;
import com.faforever.neroxis.generator.texture.WonderTextureGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.generator.util.SpawnPlacementException;
import com.faforever.neroxis.generator.util.serial.MapStyle;
import com.faforever.neroxis.generator.util.serial.PropStyle;
import com.faforever.neroxis.generator.util.serial.ResourceStyle;
import com.faforever.neroxis.generator.util.serial.TerrainStyle;
import com.faforever.neroxis.generator.util.serial.Visibility;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.SpawnPlacer;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

public class MapGenerator {
    private static final ExecutorService PLACEMENT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    static GenerationResults generate(GeneratorParameters generatorParameters,
                                      RandomGenerator.SplittableGenerator random, boolean debug, boolean visualize) {
        Predicate<HasParameterConstraints> constraintsMatchPredicate = hasConstraints -> hasConstraints.parameterConstraints()
                                                                                                       .matches(
                                                                                                               generatorParameters);
        TerrainStyle terrainStyle = generatorParameters.mapStyle()
                                                       .getTerrainStyleOptions()
                                                       .select(random, constraintsMatchPredicate);
        BiomeName biomeName = generatorParameters.mapStyle().getBiomeNameOptions().select(random);
        ResourceStyle resourceStyle = generatorParameters.mapStyle()
                                                         .getResourceStyleOptions()
                                                         .select(random, constraintsMatchPredicate);
        PropStyle propStyle = generatorParameters.mapStyle()
                                                 .getPropStyleOptions().select(random, constraintsMatchPredicate);
        SymmetrySettings symmetrySettings = generatorParameters.symmetrySettings();

        SCMap map = new SCMap(generatorParameters.mapSize(), Biomes.loadBiome(biomeName));
        map.setUnexplored(generatorParameters.visibility() == Visibility.UNEXPLORED);
        map.setGeneratePreview(generatorParameters.visibility() != Visibility.BLIND && !map.isUnexplored());

        TerrainGenerator terrainGenerator = null;
        List<Pipeline.Entry> terrainPipelineEntries = new ArrayList<>();
        while (map.getSpawnCount() != generatorParameters.spawnCount()) {
            try {
                TerrainGenerator attemptedTerrainGenerator = terrainStyle.getGeneratorSupplier().get();
                if (attemptedTerrainGenerator instanceof SpawnFirstTerrainGenerator) {
                    SpawnPlacer spawnPlacer = new SpawnPlacer(map, random.split());
                    DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeSpawns",
                                       () -> spawnPlacer.placeSpawns(generatorParameters.spawnCount(),
                                                                     getSpawnSeparation(generatorParameters, random),
                                                                     getTeamSeparation(generatorParameters),
                                                                     symmetrySettings));
                }

                terrainPipelineEntries = Pipeline.run(pipeline -> {
                    pipeline.setDebug(debug);
                    pipeline.setVisualize(visualize);
                }, () -> {
                    attemptedTerrainGenerator.initialize(map, random.split(), generatorParameters,
                                                         symmetrySettings);
                    attemptedTerrainGenerator.setupPipeline();
                });

                CompletableFuture<Void> heightMapFuture = CompletableFuture.runAsync(
                        attemptedTerrainGenerator::setHeightmapImage, PLACEMENT_EXECUTOR);

                CompletableFuture<Void> spawnFuture = CompletableFuture.runAsync(attemptedTerrainGenerator::placeSpawns,
                                                                                 PLACEMENT_EXECUTOR);

                CompletableFuture.allOf(heightMapFuture, spawnFuture).join();
                terrainGenerator = attemptedTerrainGenerator;
            } catch (CompletionException exception) {
                switch (exception.getCause()) {
                    case SpawnPlacementException _ ->
                            System.out.println("Unable to place all spawns trying new generation");
                    case Throwable _ -> throw exception;
                    case null -> throw exception;
                }
            }
        }
        if (terrainGenerator == null) {
            throw new IllegalStateException("Unable to generate terrain");
        }
        TerrainGenerator actualTerrainGenerator = terrainGenerator;

        TextureGenerator textureGenerator = switch (biomeName) {
            case BRIMSTONE -> new BrimstoneTextureGenerator();
            case DESERT -> new DesertTextureGenerator();
            case EARLYAUTUMN -> new EarlyAutumnTextureGenerator();
            case FRITHEN -> new FrithenTextureGenerator();
            case MARS -> new MarsTextureGenerator();
            case MOONLIGHT -> new MoonlightTextureGenerator();
            case PRAYER -> new PrayerTextureGenerator();
            case STONES -> new StonesTextureGenerator();
            case SUNSET -> new SunsetTextureGenerator();
            case SYRTIS -> new SyrtisTextureGenerator();
            case WINDINGRIVER -> new WindingRiverTextureGenerator();
            case WONDER -> new WonderTextureGenerator();
            case CRYSTALLINE -> new CrystallineTextureGenerator();
        };
        ResourceGenerator resourceGenerator = resourceStyle.getGeneratorSupplier().get();
        PropGenerator propGenerator = propStyle.getGeneratorSupplier().get();
        if (generatorParameters.mapStyle() instanceof MapStyle.Custom(
                _, _, _, _, float reclaimDensity, float resourceDensity
        )) {
            resourceGenerator.setResourceDensity(resourceDensity);
            propGenerator.setReclaimDensity(reclaimDensity);
        }

        List<Pipeline.Entry> placementPipelineEntries = Pipeline.run(pipeline -> {
            pipeline.setDebug(debug);
            pipeline.setVisualize(visualize);
        }, () -> {
            textureGenerator.initialize(map, random.split(), generatorParameters,
                                        new SymmetrySettings(Symmetry.NONE), actualTerrainGenerator);
            resourceGenerator.initialize(map, random.split(), generatorParameters, symmetrySettings,
                                         actualTerrainGenerator);
            propGenerator.initialize(map, random.split(), generatorParameters, symmetrySettings,
                                     actualTerrainGenerator);

            resourceGenerator.setupPipeline();
            textureGenerator.setupPipeline();
            propGenerator.setupPipeline();
        });

        CompletableFuture<Void> textureFuture = CompletableFuture.runAsync(textureGenerator::setTextures,
                                                                           PLACEMENT_EXECUTOR);
        CompletableFuture<Void> normalFuture = CompletableFuture.runAsync(textureGenerator::setCompressedDecals,
                                                                          PLACEMENT_EXECUTOR);

        CompletableFuture<Void> resourcesFuture = CompletableFuture.runAsync(resourceGenerator::placeResources,
                                                                             PLACEMENT_EXECUTOR);
        CompletableFuture<Void> decalsFuture = CompletableFuture.runAsync(textureGenerator::placeDecals,
                                                                          PLACEMENT_EXECUTOR);
        CompletableFuture<Void> propsFuture = resourcesFuture.thenRunAsync(propGenerator::placeProps,
                                                                           PLACEMENT_EXECUTOR);
        CompletableFuture<Void> unitsFuture = resourcesFuture.thenRunAsync(propGenerator::placeUnits,
                                                                           PLACEMENT_EXECUTOR);

        CompletableFuture<Void> previewFuture = propsFuture.thenRunAsync(textureGenerator::generatePreview,
                                                                         PLACEMENT_EXECUTOR);

        CompletableFuture.allOf(textureFuture, previewFuture, resourcesFuture, decalsFuture, propsFuture, unitsFuture,
                                normalFuture)
                         .thenRunAsync(() -> DebugUtil.timedRun("com.faforever.neroxis.map.generator", "setHeights",
                                                                map::setHeights), PLACEMENT_EXECUTOR)
                         .join();

        return new GenerationResults(map, symmetrySettings, terrainStyle, propStyle, biomeName, resourceStyle,
                                     terrainPipelineEntries,
                                     placementPipelineEntries);
    }

    private static float getSpawnSeparation(GeneratorParameters generatorParameters, RandomGenerator random) {
        int mapSize = generatorParameters.mapSize();
        int numTeams = generatorParameters.numTeams();
        int spawnCount = generatorParameters.spawnCount();

        if (numTeams < 2) {
            return (float) mapSize / spawnCount * 1.5f;
        } else if (numTeams == 2) {
            return random.nextInt(mapSize / 4 - mapSize / 16) + mapSize / 16f;
        } else if (numTeams < 8) {
            return random.nextInt(mapSize / 2 / numTeams - mapSize / 16) + mapSize / 16f;
        } else {
            return 0;
        }
    }

    private static int getTeamSeparation(GeneratorParameters generatorParameters) {
        int numTeams = generatorParameters.numTeams();
        int mapSize = generatorParameters.mapSize();
        if (numTeams < 2) {
            return 0;
        } else if (numTeams == 2) {
            return mapSize / 2;
        } else {
            return StrictMath.min(mapSize / numTeams, 256);
        }
    }
}
