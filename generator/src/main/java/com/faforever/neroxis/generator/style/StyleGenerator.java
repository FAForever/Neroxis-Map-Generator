package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ElementGenerator;
import com.faforever.neroxis.generator.GeneratorOptions;
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
import com.faforever.neroxis.generator.util.GeneratorSelector;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.SpawnPlacer;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.SymmetrySelector;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public abstract class StyleGenerator extends ElementGenerator {
    private TerrainGenerator terrainGenerator;
    private TextureGenerator textureGenerator;
    private ResourceGenerator resourceGenerator;
    private PropGenerator propGenerator;
    private DecalGenerator decalGenerator;
    private SpawnPlacer spawnPlacer;

    protected GeneratorOptions<TerrainGenerator> getTerrainGeneratorOptions() {
        return new GeneratorOptions<>(new BasicTerrainGenerator());
    }

    protected GeneratorOptions<TextureGenerator> getTextureGeneratorOptions() {
        return new GeneratorOptions<>(new BasicTextureGenerator());
    }

    protected GeneratorOptions<ResourceGenerator> getResourceGeneratorOptions() {
        return new GeneratorOptions<>(new BasicResourceGenerator());
    }

    protected GeneratorOptions<PropGenerator> getPropGeneratorOptions() {
        return new GeneratorOptions<>(new BasicPropGenerator());
    }

    protected GeneratorOptions<DecalGenerator> getDecalGeneratorOptions() {
        return new GeneratorOptions<>(new BasicDecalGenerator());
    }

    protected float getSpawnSeparation() {
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

    protected int getTeamSeparation() {
        if (generatorParameters.numTeams() < 2) {
            return 0;
        } else if (generatorParameters.numTeams() == 2) {
            return map.getSize() / 2;
        } else {
            return StrictMath.min(map.getSize() / generatorParameters.numTeams(), 256);
        }
    }

    public SCMap generate(GeneratorParameters generatorParameters, long seed) {
        initialize(generatorParameters, seed);
        setupPipeline();

        random = null;

        Pipeline.start();

        CompletableFuture<Void> heightMapFuture = CompletableFuture.runAsync(terrainGenerator::setHeightmapImage);
        CompletableFuture<Void> textureFuture = CompletableFuture.runAsync(textureGenerator::setTextures);
        CompletableFuture<Void> previewFuture = CompletableFuture.runAsync(textureGenerator::generatePreview);
        CompletableFuture<Void> resourcesFuture = CompletableFuture.runAsync(resourceGenerator::placeResources);
        CompletableFuture<Void> decalsFuture = CompletableFuture.runAsync(decalGenerator::placeDecals);
        CompletableFuture<Void> propsFuture = resourcesFuture.thenAccept(aVoid -> propGenerator.placeProps());
        CompletableFuture<Void> unitsFuture = resourcesFuture.thenAccept(aVoid -> propGenerator.placeUnits());

        CompletableFuture<Void> placementFuture = CompletableFuture.allOf(heightMapFuture, textureFuture, previewFuture,
                                                                          resourcesFuture, decalsFuture, propsFuture,
                                                                          unitsFuture)
                                                                   .thenAccept(aVoid -> setHeights());

        placementFuture.join();
        Pipeline.join();

        return map;
    }

    private void initialize(GeneratorParameters generatorParameters, long seed) {
        this.generatorParameters = generatorParameters;
        random = new Random(seed);
        symmetrySettings = SymmetrySelector.getSymmetrySettingsFromTerrainSymmetry(random,
                                                                                   generatorParameters.terrainSymmetry(),
                                                                                   generatorParameters.spawnCount(),
                                                                                   generatorParameters.numTeams());
        map = new SCMap(generatorParameters.mapSize(), generatorParameters.biome());
        map.setUnexplored(generatorParameters.visibility() == Visibility.UNEXPLORED);
        map.setGeneratePreview(generatorParameters.visibility() != Visibility.BLIND && !map.isUnexplored());

        Pipeline.reset();

        spawnPlacer = new SpawnPlacer(map, random.nextLong());
    }

    @Override
    public void setupPipeline() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeSpawns",
                           () -> spawnPlacer.placeSpawns(generatorParameters.spawnCount(), getSpawnSeparation(),
                                                         getTeamSeparation(), symmetrySettings));

        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "selectGenerators", () -> {
            terrainGenerator = GeneratorSelector.selectRandomMatchingGenerator(random, getTerrainGeneratorOptions(),
                                                                               generatorParameters);
            textureGenerator = GeneratorSelector.selectRandomMatchingGenerator(random, getTextureGeneratorOptions(),
                                                                               generatorParameters);
            resourceGenerator = GeneratorSelector.selectRandomMatchingGenerator(random, getResourceGeneratorOptions(),
                                                                                generatorParameters);
            propGenerator = GeneratorSelector.selectRandomMatchingGenerator(random, getPropGeneratorOptions(),
                                                                            generatorParameters);
            decalGenerator = GeneratorSelector.selectRandomMatchingGenerator(random, getDecalGeneratorOptions(),
                                                                             generatorParameters);
        });

        terrainGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings);
        terrainGenerator.setupPipeline();

        textureGenerator.initialize(map, random.nextLong(), generatorParameters, new SymmetrySettings(Symmetry.NONE),
                                    terrainGenerator);
        resourceGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings, terrainGenerator);
        propGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings, terrainGenerator);
        decalGenerator.initialize(map, random.nextLong(), generatorParameters, symmetrySettings, terrainGenerator);

        resourceGenerator.setupPipeline();
        textureGenerator.setupPipeline();
        propGenerator.setupPipeline();
        decalGenerator.setupPipeline();
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
                                 resourceGenerator.getClass().getSimpleName(), propGenerator.getClass().getSimpleName(),
                                 decalGenerator.getClass().getSimpleName());
        } else {
            return "";
        }
    }
}
