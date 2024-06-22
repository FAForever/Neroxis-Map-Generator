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
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.SymmetrySelector;
import lombok.Getter;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public abstract class StyleGenerator implements HasParameterConstraints {
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
                                              new WeightedOption<>(new MoonlightTextureGenerator(), 1f),
                                              new WeightedOption<>(new PrayerTextureGenerator(), 1f),
                                              new WeightedOption<>(new StonesTextureGenerator(), 1f),
                                              new WeightedOption<>(new SunsetTextureGenerator(), 1f),
                                              new WeightedOption<>(new SyrtisTextureGenerator(), 1f),
                                              new WeightedOption<>(new WindingRiverTextureGenerator(), 1f),
                                              new WeightedOption<>(new WonderTextureGenerator(), 1f));
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

        random = null;

        CompletableFuture<Void> placementFuture = CompletableFuture.allOf(terrainGenerator.getHeightmapSetFuture(),
                                                                          terrainGenerator.getSpawnsSetFuture(),
                                                                          textureGenerator.getTexturesSetFuture(),
                                                                          textureGenerator.getCompressedDecalsSetFuture(),
                                                                          textureGenerator.getPreviewGeneratedFuture(),
                                                                          resourceGenerator.getResourcesPlacedFuture(),
                                                                          decalGenerator.getDecalsPlacedFuture(),
                                                                          propGenerator.getPropsPlacedFuture(),
                                                                          propGenerator.getUnitsPlacedFuture())
                                                                   .thenAccept(aVoid -> setHeights());

        Pipeline.start();
        Pipeline.join();
        placementFuture.join();

        return map;
    }

    private void initialize(GeneratorParameters generatorParameters, long seed) {
        Pipeline.reset();

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

        terrainGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings);
        textureGenerator.initialize(map, random.nextLong(), generatorParameters, new SymmetrySettings(Symmetry.NONE),
                                    terrainGenerator);
        resourceGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings, terrainGenerator);
        propGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings, terrainGenerator,
                                 resourceGenerator);
        decalGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings, terrainGenerator);
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
                   """.formatted(terrainGenerator.getClass().getSimpleName(),
                                 textureGenerator.getClass().getSimpleName(),
                                 resourceGenerator.getClass().getSimpleName(),
                                 propGenerator.getClass().getSimpleName(),
                                 decalGenerator.getClass().getSimpleName());
        } else {
            return "";
        }
    }
}
