package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ElementGenerator;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.Visibility;
import com.faforever.neroxis.generator.decal.BasicDecalGenerator;
import com.faforever.neroxis.generator.decal.DecalGenerator;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.resource.BasicResourceGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.texture.BasicTextureGenerator;
import com.faforever.neroxis.generator.texture.TextureGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.placement.AIMarkerPlacer;
import com.faforever.neroxis.map.placement.SpawnPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.SymmetrySelector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class StyleGenerator extends ElementGenerator {
    protected final List<TerrainGenerator> terrainGenerators = new ArrayList<>();
    protected final List<TextureGenerator> textureGenerators = new ArrayList<>();
    protected final List<ResourceGenerator> resourceGenerators = new ArrayList<>();
    protected final List<PropGenerator> propGenerators = new ArrayList<>();
    protected final List<DecalGenerator> decalGenerators = new ArrayList<>();
    protected TerrainGenerator terrainGenerator = new BasicTerrainGenerator();
    protected TextureGenerator textureGenerator = new BasicTextureGenerator();
    protected ResourceGenerator resourceGenerator = new BasicResourceGenerator();
    protected PropGenerator propGenerator = new BasicPropGenerator();
    protected DecalGenerator decalGenerator = new BasicDecalGenerator();
    protected float spawnSeparation;
    protected int teamSeparation;
    private SpawnPlacer spawnPlacer;

    public static <T extends ElementGenerator> T selectRandomMatchingGenerator(Random random, List<T> generators,
                                                                               GeneratorParameters generatorParameters,
                                                                               T defaultGenerator) {
        List<T> matchingGenerators = generators.stream()
                                               .filter(generator -> generator.getParameterConstraints()
                                                                             .matches(generatorParameters))
                                               .collect(Collectors.toList());
        return selectRandomGeneratorUsingWeights(random, matchingGenerators, defaultGenerator);
    }

    private static <T extends ElementGenerator> T selectRandomGeneratorUsingWeights(Random random, List<T> generators,
                                                                                    T defaultGenerator) {
        if (generators.size() > 0) {
            List<Float> weights = generators.stream().map(ElementGenerator::getWeight).collect(Collectors.toList());
            List<Float> cumulativeWeights = new ArrayList<>();
            float sum = 0;
            for (float weight : weights) {
                sum += weight;
                cumulativeWeights.add(sum);
            }
            float value = random.nextFloat() * cumulativeWeights.get(cumulativeWeights.size() - 1);
            return cumulativeWeights.stream()
                                    .filter(weight -> value <= weight)
                                    .reduce((first, second) -> first)
                                    .map(weight -> generators.get(cumulativeWeights.indexOf(weight)))
                                    .orElse(defaultGenerator);
        } else {
            return defaultGenerator;
        }
    }

    public SCMap generate(GeneratorParameters generatorParameters, long seed) {
        initialize(generatorParameters, seed);

        setupPipeline();

        random = null;

        Pipeline.start();

        CompletableFuture<Void> heightMapFuture = CompletableFuture.runAsync(terrainGenerator::setHeightmapImage);
        CompletableFuture<Void> aiMarkerFuture = CompletableFuture.runAsync(
                () -> generateAIMarkers(terrainGenerator.getPassable(), terrainGenerator.getPassableLand(),
                                        terrainGenerator.getPassableWater()));
        CompletableFuture<Void> textureFuture = CompletableFuture.runAsync(textureGenerator::setTextures);
        CompletableFuture<Void> normalFuture = CompletableFuture.runAsync(textureGenerator::setCompressedDecals);
        CompletableFuture<Void> previewFuture = CompletableFuture.runAsync(textureGenerator::generatePreview);
        CompletableFuture<Void> resourcesFuture = CompletableFuture.runAsync(resourceGenerator::placeResources);
        CompletableFuture<Void> decalsFuture = CompletableFuture.runAsync(decalGenerator::placeDecals);
        CompletableFuture<Void> propsFuture = resourcesFuture.thenAccept(aVoid -> propGenerator.placeProps());
        CompletableFuture<Void> unitsFuture = resourcesFuture.thenAccept(aVoid -> propGenerator.placeUnits());

        CompletableFuture<Void> placementFuture = CompletableFuture.allOf(heightMapFuture, aiMarkerFuture,
                                                                          textureFuture, previewFuture, resourcesFuture,
                                                                          decalsFuture, propsFuture, unitsFuture,
                                                                          normalFuture)
                                                                   .thenAccept(aVoid -> setHeights());

        placementFuture.join();
        Pipeline.join();

        return map;
    }

    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        this.generatorParameters = generatorParameters;
        random = new Random(seed);
        symmetrySettings = SymmetrySelector.getSymmetrySettingsFromTerrainSymmetry(random,
                                                                                   generatorParameters.getTerrainSymmetry(),
                                                                                   generatorParameters.getSpawnCount(),
                                                                                   generatorParameters.getNumTeams());
        map = new SCMap(generatorParameters.getMapSize(), generatorParameters.getBiome());
        map.setUnexplored(generatorParameters.getVisibility() == Visibility.UNEXPLORED);
        map.setGeneratePreview(generatorParameters.getVisibility() != Visibility.BLIND && !map.isUnexplored());

        Pipeline.reset();

        if (generatorParameters.getNumTeams() < 2) {
            spawnSeparation = (float) generatorParameters.getMapSize() / generatorParameters.getSpawnCount() * 1.5f;
            teamSeparation = 0;
        } else if (generatorParameters.getNumTeams() == 2) {
            spawnSeparation = random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16f;
            teamSeparation = map.getSize() / 2;
        } else {
            if (generatorParameters.getNumTeams() < 8) {
                spawnSeparation = random.nextInt(
                        map.getSize() / 2 / generatorParameters.getNumTeams() - map.getSize() / 16)
                                  + map.getSize() / 16f;
            } else {
                spawnSeparation = 0;
            }
            teamSeparation = StrictMath.min(map.getSize() / generatorParameters.getNumTeams(), 256);
        }

        spawnPlacer = new SpawnPlacer(map, random.nextLong());
    }

    @Override
    public void setupPipeline() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeSpawns",
                           () -> spawnPlacer.placeSpawns(generatorParameters.getSpawnCount(), spawnSeparation,
                                                         teamSeparation, symmetrySettings));

        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "selectGenerators", () -> {
            terrainGenerator = selectRandomMatchingGenerator(random, terrainGenerators, generatorParameters,
                                                             terrainGenerator);
            textureGenerator = selectRandomMatchingGenerator(random, textureGenerators, generatorParameters,
                                                             textureGenerator);
            resourceGenerator = selectRandomMatchingGenerator(random, resourceGenerators, generatorParameters,
                                                              resourceGenerator);
            propGenerator = selectRandomMatchingGenerator(random, propGenerators, generatorParameters, propGenerator);
            decalGenerator = selectRandomMatchingGenerator(random, decalGenerators, generatorParameters,
                                                           decalGenerator);
        });

        terrainGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings);
        terrainGenerator.setupPipeline();

        textureGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings, terrainGenerator);
        resourceGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings, terrainGenerator);
        propGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings, terrainGenerator);
        decalGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings, terrainGenerator);

        resourceGenerator.setupPipeline();
        textureGenerator.setupPipeline();
        propGenerator.setupPipeline();
        decalGenerator.setupPipeline();
    }

    protected void generateAIMarkers(BooleanMask passable, BooleanMask passableLand, BooleanMask passableWater) {
        Pipeline.await(passable, passableLand, passableWater);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeAIMarkers", () -> {
            CompletableFuture<Void> AmphibiousMarkers = CompletableFuture.runAsync(
                    () -> AIMarkerPlacer.placeAIMarkers(passable.getFinalMask(), map.getAmphibiousAIMarkers(),
                                                        "AmphPN%d"));
            CompletableFuture<Void> LandMarkers = CompletableFuture.runAsync(
                    () -> AIMarkerPlacer.placeAIMarkers(passableLand.getFinalMask(), map.getLandAIMarkers(),
                                                        "LandPN%d"));
            CompletableFuture<Void> NavyMarkers = CompletableFuture.runAsync(
                    () -> AIMarkerPlacer.placeAIMarkers(passableWater.getFinalMask(), map.getNavyAIMarkers(),
                                                        "NavyPN%d"));
            CompletableFuture<Void> AirMarkers = CompletableFuture.runAsync(
                    () -> AIMarkerPlacer.placeAirAIMarkers(map));
            CompletableFuture.allOf(AmphibiousMarkers, LandMarkers, NavyMarkers, AirMarkers).join();
        });
    }

    protected void setHeights() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "setPlacements", () -> map.setHeights());
    }

    public String generatorsToString() {
        if (generatorParameters.getVisibility() == null) {
            return "TerrainGenerator: "
                   + terrainGenerator.getClass().getSimpleName()
                   + "\nTextureGenerator: "
                   + textureGenerator.getClass().getSimpleName()
                   + "\nResourceGenerator: "
                   + resourceGenerator.getClass().getSimpleName()
                   + "\nPropGenerator: "
                   + propGenerator.getClass().getSimpleName()
                   + "\nDecalGenerator: "
                   + decalGenerator.getClass().getSimpleName();
        } else {
            return "";
        }
    }
}
