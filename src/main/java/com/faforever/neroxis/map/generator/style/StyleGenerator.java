package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.generator.ElementGenerator;
import com.faforever.neroxis.map.generator.decal.BasicDecalGenerator;
import com.faforever.neroxis.map.generator.decal.DecalGenerator;
import com.faforever.neroxis.map.generator.placement.AIMarkerPlacer;
import com.faforever.neroxis.map.generator.placement.SpawnPlacer;
import com.faforever.neroxis.map.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.map.generator.prop.PropGenerator;
import com.faforever.neroxis.map.generator.resource.BasicResourceGenerator;
import com.faforever.neroxis.map.generator.resource.ResourceGenerator;
import com.faforever.neroxis.map.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.generator.texture.BasicTextureGenerator;
import com.faforever.neroxis.map.generator.texture.TextureGenerator;
import com.faforever.neroxis.map.mask.BooleanMask;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.Util;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static com.faforever.neroxis.util.RandomUtils.selectRandomMatchingGenerator;

public abstract strictfp class StyleGenerator extends ElementGenerator {

    protected final List<TerrainGenerator> terrainGenerators = new ArrayList<>();
    @Getter
    protected TerrainGenerator terrainGenerator = new BasicTerrainGenerator();
    protected final List<TextureGenerator> textureGenerators = new ArrayList<>();
    @Getter
    protected TextureGenerator textureGenerator = new BasicTextureGenerator();
    protected final List<ResourceGenerator> resourceGenerators = new ArrayList<>();
    @Getter
    protected ResourceGenerator resourceGenerator = new BasicResourceGenerator();
    protected final List<PropGenerator> propGenerators = new ArrayList<>();
    @Getter
    protected PropGenerator propGenerator = new BasicPropGenerator();
    protected final List<DecalGenerator> decalGenerators = new ArrayList<>();
    @Getter
    protected DecalGenerator decalGenerator = new BasicDecalGenerator();
    @Getter
    protected String name;
    protected float spawnSeparation;
    protected int teamSeparation;
    private SpawnPlacer spawnPlacer;

    protected void initialize(MapParameters mapParameters, long seed) {
        this.mapParameters = mapParameters;
        random = new Random(seed);
        map = new SCMap(mapParameters.getMapSize(), mapParameters.getBiome());
        map.setUnexplored(mapParameters.isUnexplored());
        map.setGeneratePreview(!mapParameters.isBlind());

        Pipeline.reset();

        spawnSeparation = mapParameters.getNumTeams() > 0 ? random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16f : (float) mapParameters.getMapSize() / mapParameters.getSpawnCount();
        teamSeparation = StrictMath.min(map.getSize() * 3 / 8, 256);

        spawnPlacer = new SpawnPlacer(map, random.nextLong());
    }

    public SCMap generate(MapParameters mapParameters, long seed) {
        initialize(mapParameters, seed);

        setupPipeline();

        random = null;

        Pipeline.start();

        CompletableFuture<Void> heightMapFuture = CompletableFuture.runAsync(terrainGenerator::setHeightmapImage);
        CompletableFuture<Void> aiMarkerFuture = CompletableFuture.runAsync(() ->
                generateAIMarkers(terrainGenerator.getPassable(), terrainGenerator.getPassableLand(), terrainGenerator.getPassableWater()));
        CompletableFuture<Void> textureFuture = CompletableFuture.runAsync(textureGenerator::setTextures);
        CompletableFuture<Void> normalFuture = CompletableFuture.runAsync(textureGenerator::setCompressedNormal);
        CompletableFuture<Void> previewFuture = CompletableFuture.runAsync(textureGenerator::generatePreview);
        CompletableFuture<Void> resourcesFuture = CompletableFuture.runAsync(resourceGenerator::placeResources);
        CompletableFuture<Void> decalsFuture = CompletableFuture.runAsync(decalGenerator::placeDecals);
        CompletableFuture<Void> propsFuture = resourcesFuture.thenAccept(aVoid -> propGenerator.placeProps());
        CompletableFuture<Void> unitsFuture = resourcesFuture.thenAccept(aVoid -> propGenerator.placeUnits());

        CompletableFuture<Void> placementFuture = CompletableFuture.allOf(heightMapFuture, aiMarkerFuture, textureFuture,
                previewFuture, resourcesFuture, decalsFuture, propsFuture, unitsFuture, normalFuture)
                .thenAccept(aVoid -> setHeights());

        placementFuture.join();
        Pipeline.join();

        return map;
    }

    @Override
    public void setupPipeline() {
        Util.timedRun("com.faforever.neroxis.map.generator", "placeSpawns", () ->
                spawnPlacer.placeSpawns(mapParameters.getSpawnCount(), spawnSeparation, teamSeparation, mapParameters.getSymmetrySettings()));

        Util.timedRun("com.faforever.neroxis.map.generator", "selectGenerators", () -> {
            terrainGenerator = selectRandomMatchingGenerator(random, terrainGenerators, mapParameters, terrainGenerator);
            textureGenerator = selectRandomMatchingGenerator(random, textureGenerators, mapParameters, textureGenerator);
            resourceGenerator = selectRandomMatchingGenerator(random, resourceGenerators, mapParameters, resourceGenerator);
            propGenerator = selectRandomMatchingGenerator(random, propGenerators, mapParameters, propGenerator);
            decalGenerator = selectRandomMatchingGenerator(random, decalGenerators, mapParameters, decalGenerator);
        });

        terrainGenerator.initialize(map, random.nextLong(), mapParameters);
        terrainGenerator.setupPipeline();

        textureGenerator.initialize(map, random.nextLong(), mapParameters, terrainGenerator);
        resourceGenerator.initialize(map, random.nextLong(), mapParameters, terrainGenerator);
        propGenerator.initialize(map, random.nextLong(), mapParameters, terrainGenerator);
        decalGenerator.initialize(map, random.nextLong(), mapParameters, terrainGenerator);

        resourceGenerator.setupPipeline();
        textureGenerator.setupPipeline();
        propGenerator.setupPipeline();
        decalGenerator.setupPipeline();
    }

    protected void generateAIMarkers(BooleanMask passable, BooleanMask passableLand, BooleanMask passableWater) {
        Pipeline.await(passable, passableLand, passableWater);
        Util.timedRun("com.faforever.neroxis.map.generator", "placeAIMarkers", () -> {
            CompletableFuture<Void> AmphibiousMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAIMarkers(passable.getFinalMask(), map.getAmphibiousAIMarkers(), "AmphPN%d"));
            CompletableFuture<Void> LandMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAIMarkers(passableLand.getFinalMask(), map.getLandAIMarkers(), "LandPN%d"));
            CompletableFuture<Void> NavyMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAIMarkers(passableWater.getFinalMask(), map.getNavyAIMarkers(), "NavyPN%d"));
            CompletableFuture<Void> AirMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAirAIMarkers(map));
            CompletableFuture.allOf(AmphibiousMarkers, LandMarkers, NavyMarkers, AirMarkers).join();
        });
    }

    protected void setHeights() {
        Util.timedRun("com.faforever.neroxis.map.generator", "setPlacements", () -> map.setHeights());
    }

    public String generatorsToString() {
        return "TerrainGenerator: " + terrainGenerator.getClass().getSimpleName() +
                "\nTextureGenerator: " + textureGenerator.getClass().getSimpleName() +
                "\nResourceGenerator: " + resourceGenerator.getClass().getSimpleName() +
                "\nPropGenerator: " + propGenerator.getClass().getSimpleName() +
                "\nDecalGenerator: " + decalGenerator.getClass().getSimpleName();
    }
}
