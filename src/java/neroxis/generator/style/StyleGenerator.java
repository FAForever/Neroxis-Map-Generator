package neroxis.generator.style;

import lombok.Getter;
import neroxis.generator.AIMarkerPlacer;
import neroxis.generator.ElementGenerator;
import neroxis.generator.MapGenerator;
import neroxis.generator.SpawnPlacer;
import neroxis.generator.decal.DecalGenerator;
import neroxis.generator.decal.DefaultDecalGenerator;
import neroxis.generator.prop.DefaultPropGenerator;
import neroxis.generator.prop.PropGenerator;
import neroxis.generator.resource.DefaultResourceGenerator;
import neroxis.generator.resource.ResourceGenerator;
import neroxis.generator.terrain.DefaultTerrainGenerator;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.generator.texture.DefaultTextureGenerator;
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

public abstract strictfp class StyleGenerator extends ElementGenerator {

    public static boolean DEBUG = MapGenerator.DEBUG;
    protected final List<TerrainGenerator> terrainGenerators = new ArrayList<>();
    protected final List<TextureGenerator> textureGenerators = new ArrayList<>();
    protected final List<ResourceGenerator> resourceGenerators = new ArrayList<>();
    protected final List<PropGenerator> propGenerators = new ArrayList<>();
    protected final List<DecalGenerator> decalGenerators = new ArrayList<>();
    @Getter
    protected String name;
    protected float spawnSeparation;
    protected int teamSeparation;
    private SpawnPlacer spawnPlacer;

    public void initialize(MapParameters mapParameters, long seed) {
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

    public SCMap generate() {
        spawnPlacer.placeSpawns(spawnSeparation, teamSeparation, mapParameters.getSymmetrySettings());

        terrainGenerators.removeIf(terrainGenerator -> !terrainGenerator.getParameterConstraints().matches(mapParameters));
        textureGenerators.removeIf(textureGenerator -> !textureGenerator.getParameterConstraints().matches(mapParameters));
        resourceGenerators.removeIf(resourceGenerator -> !resourceGenerator.getParameterConstraints().matches(mapParameters));
        propGenerators.removeIf(propGenerator -> !propGenerator.getParameterConstraints().matches(mapParameters));
        decalGenerators.removeIf(decalGenerator -> !decalGenerator.getParameterConstraints().matches(mapParameters));

        TerrainGenerator terrainGenerator = terrainGenerators.size() >= 1 ? terrainGenerators.get(random.nextInt(terrainGenerators.size())) : new DefaultTerrainGenerator();
        TextureGenerator textureGenerator = textureGenerators.size() >= 1 ? textureGenerators.get(random.nextInt(textureGenerators.size())) : new DefaultTextureGenerator();
        ResourceGenerator resourceGenerator = resourceGenerators.size() >= 1 ? resourceGenerators.get(random.nextInt(resourceGenerators.size())) : new DefaultResourceGenerator();
        PropGenerator propGenerator = propGenerators.size() >= 1 ? propGenerators.get(random.nextInt(propGenerators.size())) : new DefaultPropGenerator();
        DecalGenerator decalGenerator = decalGenerators.size() >= 1 ? decalGenerators.get(random.nextInt(decalGenerators.size())) : new DefaultDecalGenerator();

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

    public void setupPipeline() {
    }

    protected void generateAIMarkers(ConcurrentBinaryMask passable, ConcurrentBinaryMask passableLand, ConcurrentBinaryMask passableWater) {
        Pipeline.await(passable, passableLand, passableWater);
        long sTime = System.currentTimeMillis();
        CompletableFuture<Void> AmphibiousMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAIMarkers(passable.getFinalMask(), map.getAmphibiousAIMarkers(), "AmphPN%d"));
        CompletableFuture<Void> LandMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAIMarkers(passableLand.getFinalMask(), map.getLandAIMarkers(), "LandPN%d"));
        CompletableFuture<Void> NavyMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAIMarkers(passableWater.getFinalMask(), map.getNavyAIMarkers(), "NavyPN%d"));
        CompletableFuture<Void> AirMarkers = CompletableFuture.runAsync(() -> AIMarkerPlacer.placeAirAIMarkers(map));
        CompletableFuture.allOf(AmphibiousMarkers, LandMarkers, NavyMarkers, AirMarkers).join();
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, placeAIMarkers\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(neroxis.generator.style.DefaultStyleGenerator.class));
        }
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
        long sTime = System.currentTimeMillis();
        map.setHeights();
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, setPlacements\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(neroxis.generator.style.DefaultStyleGenerator.class));
        }
    }
}
