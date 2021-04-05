package neroxis.generator.style;

import lombok.Getter;
import neroxis.generator.AIMarkerPlacer;
import neroxis.generator.ElementGenerator;
import neroxis.generator.SpawnPlacer;
import neroxis.generator.decal.BasicDecalGenerator;
import neroxis.generator.decal.DecalGenerator;
import neroxis.generator.prop.BasicPropGenerator;
import neroxis.generator.prop.PropGenerator;
import neroxis.generator.resource.BasicResourceGenerator;
import neroxis.generator.resource.ResourceGenerator;
import neroxis.generator.terrain.BasicTerrainGenerator;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.generator.texture.BasicTextureGenerator;
import neroxis.generator.texture.TextureGenerator;
import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;
import neroxis.util.Pipeline;
import neroxis.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static neroxis.util.RandomUtils.selectRandomMatchingGenerator;

public abstract strictfp class StyleGenerator extends ElementGenerator {

    protected final List<TerrainGenerator> terrainGenerators = new ArrayList<>();
    protected TerrainGenerator terrainGenerator = new BasicTerrainGenerator();
    protected final List<TextureGenerator> textureGenerators = new ArrayList<>();
    protected TextureGenerator textureGenerator = new BasicTextureGenerator();
    protected final List<ResourceGenerator> resourceGenerators = new ArrayList<>();
    protected ResourceGenerator resourceGenerator = new BasicResourceGenerator();
    protected final List<PropGenerator> propGenerators = new ArrayList<>();
    protected PropGenerator propGenerator = new BasicPropGenerator();
    protected final List<DecalGenerator> decalGenerators = new ArrayList<>();
    protected DecalGenerator decalGenerator = new BasicDecalGenerator();
    @Getter
    protected String name;
    protected float spawnSeparation;
    protected int teamSeparation;
    private SpawnPlacer spawnPlacer;

    protected void initialize(MapParameters mapParameters, long seed) {
        this.mapParameters = mapParameters;
        random = new Random(seed);
        map = new SCMap(mapParameters.getMapSize(), mapParameters.getSpawnCount(),
                getMexCount(), mapParameters.getHydroCount(),
                mapParameters.getBiome());

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
        CompletableFuture<Void> resourcesFuture = CompletableFuture.runAsync(resourceGenerator::placeResources);
        CompletableFuture<Void> decalsFuture = CompletableFuture.runAsync(decalGenerator::placeDecals);
        CompletableFuture<Void> propsFuture = resourcesFuture.thenAccept(aVoid -> propGenerator.placeProps());
        CompletableFuture<Void> unitsFuture = resourcesFuture.thenAccept(aVoid -> propGenerator.placeUnits());

        CompletableFuture<Void> placementFuture = CompletableFuture.allOf(heightMapFuture, aiMarkerFuture, textureFuture,
                resourcesFuture, decalsFuture, propsFuture, unitsFuture)
                .thenAccept(aVoid -> setHeights());

        placementFuture.join();
        Pipeline.join();

        return map;
    }

    @Override
    public void setupPipeline() {
        Util.timedRun("neroxis.generator", "placeSpawns", () -> {
            spawnPlacer.placeSpawns(spawnSeparation, teamSeparation, mapParameters.getSymmetrySettings());
        });

        Util.timedRun("neroxis.generator", "selectGenerators", () -> {
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

    protected void generateAIMarkers(ConcurrentBinaryMask passable, ConcurrentBinaryMask passableLand, ConcurrentBinaryMask passableWater) {
        Pipeline.await(passable, passableLand, passableWater);
        Util.timedRun("neroxis.generator", "placeAIMarkers", () -> {
            CompletableFuture<Void> AmphibiousMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAIMarkers(passable.getFinalMask(), map.getAmphibiousAIMarkers(), "AmphPN%d"));
            CompletableFuture<Void> LandMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAIMarkers(passableLand.getFinalMask(), map.getLandAIMarkers(), "LandPN%d"));
            CompletableFuture<Void> NavyMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAIMarkers(passableWater.getFinalMask(), map.getNavyAIMarkers(), "NavyPN%d"));
            CompletableFuture<Void> AirMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAirAIMarkers(map));
            CompletableFuture.allOf(AmphibiousMarkers, LandMarkers, NavyMarkers, AirMarkers).join();
        });
    }

    protected int getMexCount() {
        int mexCount;
        int mapSize = mapParameters.getMapSize();
        int spawnCount = mapParameters.getSpawnCount();
        float mexDensity = mapParameters.getMexDensity();
        float mexMultiplier = 1f;
        if (spawnCount <= 2) {
            mexCount = (int) (10 + 20 * mexDensity);
        } else if (spawnCount <= 4) {
            mexCount = (int) (9 + 8 * mexDensity);
        } else if (spawnCount <= 10) {
            mexCount = (int) (8 + 4 * mexDensity);
        } else if (spawnCount <= 12) {
            mexCount = (int) (6 + 7 * mexDensity);
        } else {
            mexCount = (int) (6 + 6 * mexDensity);
        }
        if (mapSize < 512) {
            mexMultiplier = .75f;
        } else if (mapSize > 512) {
            if (spawnCount <= 6) {
                mexMultiplier = 1.5f;
            } else if (spawnCount <= 10) {
                mexMultiplier = 1.35f;
            } else {
                mexMultiplier = 1.25f;
            }
        }
        mexCount *= mexMultiplier;
        return mexCount * spawnCount;
    }

    protected void setHeights() {
        Util.timedRun("neroxis.generator", "setPlacements", () -> {
            map.setHeights();
        });
    }
}
