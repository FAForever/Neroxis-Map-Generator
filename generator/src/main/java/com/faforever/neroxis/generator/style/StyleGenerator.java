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
import com.faforever.neroxis.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.texture.BrimstoneTextureGenerator;
import com.faforever.neroxis.generator.texture.CrystallineTextureGenerator;
import com.faforever.neroxis.generator.texture.DesertTextureGenerator;
import com.faforever.neroxis.generator.texture.EarlyAutumnTextureGenerator;
import com.faforever.neroxis.generator.texture.FrithenTextureGenerator;
import com.faforever.neroxis.generator.texture.MarsTextureGenerator;
import com.faforever.neroxis.generator.texture.PrayerTextureGenerator;
import com.faforever.neroxis.generator.texture.StonesTextureGenerator;
import com.faforever.neroxis.generator.texture.SyrtisTextureGenerator;
import com.faforever.neroxis.generator.texture.TextureGenerator;
import com.faforever.neroxis.generator.texture.WindingRiverTextureGenerator;
import com.faforever.neroxis.generator.texture.WonderTextureGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.SymmetrySelector;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public abstract class StyleGenerator implements HasParameterConstraints {
    private static final ExecutorService PLACEMENT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final Pipeline terrainPipeline = new Pipeline();
    private final Pipeline placementPipeline = new Pipeline();

    private TerrainGenerator terrainGenerator;
    private TextureGenerator textureGenerator;
    private ResourceGenerator resourceGenerator;
    private PropGenerator propGenerator;
    private DecalGenerator decalGenerator;
    private SCMap map;
    private Random random;

    @Getter
    private GeneratorParameters generatorParameters;
    @Getter
    private SymmetrySettings symmetrySettings;

    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicTerrainGenerator());
    }

    protected WeightedOptionsWithFallback<TextureGenerator> getTextureGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BrimstoneTextureGenerator(),
                                              new WeightedOption<>(new DesertTextureGenerator(), 1f),
                                              new WeightedOption<>(new EarlyAutumnTextureGenerator(), 1f),
                                              new WeightedOption<>(new FrithenTextureGenerator(), 1f),
                                              new WeightedOption<>(new MarsTextureGenerator(), 1f),
                                              new WeightedOption<>(new PrayerTextureGenerator(), 1f),
                                              new WeightedOption<>(new StonesTextureGenerator(), 1f),
                                              // new WeightedOption<>(new SunsetTextureGenerator(), 1f),
                                              new WeightedOption<>(new SyrtisTextureGenerator(), 1f),
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

    public SCMap generate(GeneratorParameters generatorParameters, long seed) {
        initialize(generatorParameters, seed);

        terrainGenerator.initialize(map, random.nextLong(), this.generatorParameters, symmetrySettings,
                                    terrainPipeline);
        terrainGenerator.setupPipeline();

        terrainPipeline.start();

        CompletableFuture<Void> heightMapFuture = CompletableFuture.runAsync(terrainGenerator::setHeightmapImage,
                                                                             PLACEMENT_EXECUTOR);
        CompletableFuture<Void> spawnFuture = CompletableFuture.runAsync(terrainGenerator::placeSpawns,
                                                                         PLACEMENT_EXECUTOR);

        terrainPipeline.join();
        CompletableFuture.allOf(heightMapFuture, spawnFuture).join();

        textureGenerator.initialize(map, random.nextLong(), this.generatorParameters,
                                    new SymmetrySettings(Symmetry.NONE),
                                    terrainGenerator, placementPipeline);
        resourceGenerator.initialize(map, random.nextLong(), this.generatorParameters, symmetrySettings,
                                     terrainGenerator,
                                     placementPipeline);
        propGenerator.initialize(map, random.nextLong(), this.generatorParameters, symmetrySettings, terrainGenerator,
                                 placementPipeline);
        decalGenerator.initialize(map, random.nextLong(), this.generatorParameters, symmetrySettings, terrainGenerator,
                                  placementPipeline);

        resourceGenerator.setupPipeline();
        textureGenerator.setupPipeline();
        propGenerator.setupPipeline();
        decalGenerator.setupPipeline();

        random = null;

        placementPipeline.start();

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

        CompletableFuture<Void> placementFuture = CompletableFuture.allOf(textureFuture, previewFuture,
                                                                          resourcesFuture, decalsFuture, propsFuture,
                                                                          unitsFuture, normalFuture)
                                                                   .thenRunAsync(this::setHeights, PLACEMENT_EXECUTOR);

        placementPipeline.join();
        placementFuture.join();

        return map;
    }

    private void initialize(GeneratorParameters generatorParameters, long seed) {
        random = new Random(seed);
        this.generatorParameters = generatorParameters;
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "selectGenerators", () -> {
            Predicate<HasParameterConstraints> constraintsMatchPredicate = hasConstraints -> hasConstraints.getParameterConstraints()
                                                                                                           .matches(
                                                                                                                   generatorParameters);
            terrainGenerator = getTerrainGeneratorOptions().select(random, constraintsMatchPredicate);
            textureGenerator = getTextureGeneratorOptions().select(random, constraintsMatchPredicate);
            resourceGenerator = getResourceGeneratorOptions().select(random, constraintsMatchPredicate);
            propGenerator = getPropGeneratorOptions().select(random, constraintsMatchPredicate);
            decalGenerator = getDecalGeneratorOptions().select(random, constraintsMatchPredicate);
        });

        symmetrySettings = SymmetrySelector.getSymmetrySettingsFromTerrainSymmetry(random,
                                                                                   generatorParameters.terrainSymmetry(),
                                                                                   generatorParameters.spawnCount(),
                                                                                   generatorParameters.numTeams());
        map = new SCMap(generatorParameters.mapSize(), textureGenerator.loadBiome());
        map.setUnexplored(generatorParameters.visibility() == Visibility.UNEXPLORED);
        map.setGeneratePreview(generatorParameters.visibility() != Visibility.BLIND && !map.isUnexplored());
    }

    protected void setHeights() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "setPlacements", () -> map.setHeights());
    }

    public String generatorsToString() {
        if (generatorParameters.visibility() == null) {
            return """
                   TerrainGenerator: %s
                   TextureGenerator: %s
                   ResourceGenerator: %s
                   PropGenerator: %s
                   DecalGenerator: %s
                   Resource Density: %s
                   Reclaim Density: %s
                   """.formatted(terrainGenerator.getClass().getSimpleName(),
                                 textureGenerator.getClass().getSimpleName(),
                                 resourceGenerator.getClass().getSimpleName(),
                                 propGenerator.getClass().getSimpleName(),
                                 decalGenerator.getClass().getSimpleName(),
                                 resourceGenerator.getResourceDensity(),
                                 propGenerator.getReclaimDensity());
        } else {
            return "";
        }
    }

    public void setDebug(boolean debug) {
        placementPipeline.setDebug(debug);
        terrainPipeline.setDebug(debug);
    }

    public void setHashMasks(boolean hashMasks) {
        placementPipeline.setHashMasks(hashMasks);
        terrainPipeline.setHashMasks(hashMasks);
    }

    public final void writePipelines(OutputStream out) throws IOException {
        terrainPipeline.write(out);
        placementPipeline.write(out);
    }

    public void setVisualize(boolean visualize) {
        terrainPipeline.setVisualize(visualize);
        placementPipeline.setVisualize(visualize);
    }
}
