package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.FractalWaterMasks;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.Vertex;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;
import java.util.random.RandomGenerator;

public class SetonishLastTerrainGenerator extends FractalNoiseLastTerrainGenerator {
    public static final float WATER_LAYER_MAX_HEIGHT = 3.0f;
    public static final float WATER_LAYER_MIN_HEIGHT = 0.0f;

    BooleanMask landBridgeBrush;
    FloatMask mexDeadZoneNoise;

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        landBridgeBrush = new BooleanMask(1, random.split(), symmetrySettings, "mapWithBridge");
        mexDeadZoneNoise = new FloatMask(1, random.split(), symmetrySettings, "mexDeadZoneNoise");

        fractalParams = FractalParams.builder()
                                     .waterHeight(15.0f)
                                     .fractalWaterMask(FractalWaterMasks.SETONS)
                                     .noiseMapBlurAmount(4)
                                     .noiseSmallestDetail(4)
                                     .noiseOctaveMultiplier(1.5f)
                                     .noiseExpMultiplier(5.0f)
                                     .teamSeparation(2)
                                     .spawnMaskDeflate(16)
                                     .clampMapHeight(50.0f)
                                     .fractalFlattenParams(List.of(
                                             FractalFlattenParams.builder()
                                                                 .minHeight(WATER_LAYER_MIN_HEIGHT)
                                                                 .maxHeight(WATER_LAYER_MAX_HEIGHT)
                                                                 .destinationMinHeight(6.0f)
                                                                 .destinationMaxHeight(16.0f)
                                                                 .slope(8.0f)
                                                                 .edgeBlur(0)
                                                                 .hasRamps(true)
                                                                 .rampPercentage(0.25f)
                                                                 .spawnable(false)
                                                                 .spawnMaskDeflate(4)
                                                                 .build(),
                                             FractalFlattenParams.builder()
                                                                 .minHeight(3.0f)
                                                                 .maxHeight(30.0f)
                                                                 .destinationMinHeight(17.0f)
                                                                 .destinationMaxHeight(17.0f)
                                                                 .slope(2.0f)
                                                                 .edgeBlur(1)
                                                                 .hasRamps(false)
                                                                 .rampPercentage(0.0f)
                                                                 .spawnable(true)
                                                                 .spawnMaskDeflate(4)
                                                                 .build(),
                                             FractalFlattenParams.builder()
                                                                 .minHeight(30.0f)
                                                                 .maxHeight(50.0f)
                                                                 .destinationMinHeight(17.0f)
                                                                 .destinationMaxHeight(20.0f)
                                                                 .slope(0.75f)
                                                                 .edgeBlur(1)
                                                                 .hasRamps(false)
                                                                 .rampPercentage(0.0f)
                                                                 .spawnable(false)
                                                                 .spawnMaskDeflate(4)
                                                                 .build()
                                     ))
                                     .build();
        super.initialize(map, random, generatorParameters, symmetrySettings);
    }

    @Override
    protected void addWaterAreasToNoiseMap(int mapSize) {
        float waterStrength = random.nextFloat(1.3f, 2.5f);

        int bridgeSize = mapSize / 5;
        int landRectanglePadding = mapSize / 16;
        int landRectangleWidthAndHeight = (mapSize / 2) + landRectanglePadding;
        int halfMapSize = (mapSize / 2) + 1;
        Vector2 island = null;
        int islandSize = mapSize / 16;
        int islandPadding = mapSize / 6;

        switch (symmetrySettings.teamSymmetry()) {
            case POINT2, DIAG, XZ -> {
                waterArea.fillRect(0, 0, landRectangleWidthAndHeight, landRectangleWidthAndHeight, true);
                waterArea.fillRect(halfMapSize - landRectanglePadding, halfMapSize - landRectanglePadding,
                                   landRectangleWidthAndHeight, landRectangleWidthAndHeight, true);
                bridgeLandArea.fillQuadrilateral(new Vertex(halfMapSize - (bridgeSize / 2) - landRectanglePadding,
                                                            halfMapSize + landRectanglePadding),
                                                 new Vertex(halfMapSize + landRectanglePadding,
                                                            halfMapSize - (bridgeSize / 2) - landRectanglePadding),
                                                 new Vertex(halfMapSize + (bridgeSize / 2) + landRectanglePadding,
                                                            halfMapSize - landRectanglePadding),
                                                 new Vertex(halfMapSize - landRectanglePadding,
                                                            halfMapSize + (bridgeSize / 2) + landRectanglePadding),
                                                 true);
                island = new Vector2((float) islandSize / 2,
                                     random.nextFloat(0, ((float) halfMapSize) - islandPadding));
            }
            case ZX -> {
                waterArea.fillRect(halfMapSize - landRectanglePadding, 0, landRectangleWidthAndHeight,
                                   landRectangleWidthAndHeight, true);
                waterArea.fillRect(0, halfMapSize - landRectanglePadding, landRectangleWidthAndHeight,
                                   landRectangleWidthAndHeight, true);
                bridgeLandArea.fillQuadrilateral(new Vertex(halfMapSize - (bridgeSize / 2) - landRectanglePadding,
                                                            halfMapSize - landRectanglePadding),
                                                 new Vertex(halfMapSize - landRectanglePadding,
                                                            halfMapSize - (bridgeSize / 2) - landRectanglePadding),
                                                 new Vertex(halfMapSize + (bridgeSize / 2) + landRectanglePadding,
                                                            halfMapSize + landRectanglePadding),
                                                 new Vertex(halfMapSize + landRectanglePadding,
                                                            halfMapSize + (bridgeSize / 2) + landRectanglePadding),
                                                 true);
                island = new Vector2((float) islandSize / 2,
                                     random.nextFloat((float) halfMapSize + islandPadding, mapSize));
            }
            case X -> {
                waterArea.fillTriangle(new Vertex(-landRectanglePadding, 0),
                                       new Vertex(mapSize + landRectanglePadding, 0),
                                       new Vertex(halfMapSize, halfMapSize),
                                       true);
                waterArea.fillTriangle(new Vertex(-landRectanglePadding, mapSize),
                                       new Vertex(mapSize + landRectanglePadding, mapSize),
                                       new Vertex(halfMapSize, halfMapSize),
                                       true);
                bridgeLandArea.fillRect(halfMapSize - (bridgeSize / 2) - landRectanglePadding,
                                        halfMapSize - (bridgeSize / 2), bridgeSize + (landRectanglePadding * 2),
                                        bridgeSize, true);
                island = new Vector2(random.nextFloat(islandPadding, mapSize - islandPadding), (float) islandSize / 2);
            }
            case Z -> {
                waterArea.fillTriangle(new Vertex(0, -landRectanglePadding),
                                       new Vertex(0, mapSize + landRectanglePadding),
                                       new Vertex(halfMapSize, halfMapSize),
                                       true);
                waterArea.fillTriangle(new Vertex(mapSize, -landRectanglePadding),
                                       new Vertex(mapSize, mapSize + landRectanglePadding),
                                       new Vertex(halfMapSize, halfMapSize),
                                       true);
                bridgeLandArea.fillRect(halfMapSize - (bridgeSize / 2),
                                        halfMapSize - (bridgeSize / 2) - landRectanglePadding, bridgeSize,
                                        bridgeSize + (landRectanglePadding * 2), true);
                island = new Vector2((float) islandSize / 2, random.nextFloat(islandPadding, mapSize - islandPadding));
            }
            case NONE -> {
                // lets do nothing
            }
            default -> {
                waterArea.drawSymmetryLines(symmetrySettings.teamSymmetry());
                waterArea.inflate((int) (mapSize / 4f / symmetrySettings.teamSymmetry().getNumSymPoints()));
                bridgeLandArea.fillCircle(new Vector2((float) halfMapSize, (float) halfMapSize), mapSize / 10f, true);
            }
        }

        waterArea.setToValue(bridgeLandArea, false);

        waterAreaMinusIsland = waterArea.copy();
        if (island != null) {
            List<Vector2> symmetryPoints = waterAreaMinusIsland.getSymmetryPointsWithOutOfBounds(island,
                                                                                                 SymmetryType.SPAWN)
                                                               .stream()
                                                               .map(Vector2::roundToNearestHalfPoint)
                                                               .toList();
            waterAreaMinusIsland.fillCircle(island, islandSize, false);
            symmetryPoints.forEach(s -> waterAreaMinusIsland.fillCircle(s, islandSize, false));
        }

        waterAreaBlur = waterAreaMinusIsland.copyAsFloatMask(0f, waterStrength);
        waterAreaBlur.blur(mapSize / 16);

        landNoiseMap.add(1.2f);
        landNoiseMap.subtract(waterAreaBlur);
    }

    @Override
    protected void blurRamps() {
        BooleanMask inflatedRamps = ramps.copy();

        heightmap.blur(4, inflatedRamps.copy().inflate(4))
                 .blur(4, inflatedRamps.copy().inflate(4).outline().inflate(2))
                 .blur(2, inflatedRamps.copy().inflate(4).outline().inflate(4))

                 .blur(8, inflatedRamps.copy().inflate(8))
                 .blur(2, inflatedRamps.copy().inflate(8).outline().inflate(4))

                 .blur(16, inflatedRamps.copy().inflate(16))
                 .blur(2, inflatedRamps.copy().inflate(16).outline().inflate(4))

                 .blur(4, inflatedRamps.copy().inflate(32))
                 .blur(4, inflatedRamps.copy().inflate(32))
                 .blur(2, inflatedRamps.copy().inflate(32).outline().inflate(4))

                 .clampMin(0f)
                 .clampMax(255f);
    }

    @Override
    protected void setupMountainHeightmapPipeline() {
        int mapSize = map.getSize();
        rawMountains.setSize(mapSize + 1);

        String brushName = Brushes.CLEAN_MOUNTAIN_BRUSHES.get(random.nextInt(Brushes.CLEAN_MOUNTAIN_BRUSHES.size()));

        // Draw some mountains on the water edges
        BooleanMask waterEdgeMountainArea = landNoiseMap
                .copyAsBooleanMask(WATER_LAYER_MIN_HEIGHT, WATER_LAYER_MAX_HEIGHT)
                .outline()
                .subtract(rampNoise.copyAsBooleanMask(.2f))
                .limitToSymmetryRegion()
                .inflate(7);
        rawMountains.useBrushWithinAreaWithDensity(waterEdgeMountainArea, brushName, 15, 7f, 1.2f, false);

        // Draw some mountains at the back of the land masses for large maps
        if (mapSize > 768) {
            BooleanMask rearMountainArea = landNoiseMap
                    .copyAsBooleanMask(WATER_LAYER_MAX_HEIGHT)
                    .subtract(waterArea)
                    .fillCircle(mapSize / 2f, mapSize / 2f, mapSize / 1.7f, false)
                    .limitToSymmetryRegion();

            int numMountainsToDraw = random.nextInt(5, 10);
            for (int i = 0; i < numMountainsToDraw; i++) {
                brushName = Brushes.CLEAN_MOUNTAIN_BRUSHES.get(random.nextInt(Brushes.CLEAN_MOUNTAIN_BRUSHES.size()));
                rawMountains.useBrushWithinArea(rearMountainArea, brushName, 100, 1, 15, false);
            }
        }
    }

    @Override
    protected void setupHeightmapPipeline() {
        // This extra step in the heightmap pipeline creates islands in the water area
        // It raises the underwater mountains to be above water
        if (waterMask != FractalWaterMasks.NONE) {
            landNoiseMap.multiply(waterAreaBlur.copy().add(1f).scaleExponentially(1.1f));
            landNoiseMap.clampMax(fractalParams.clampMapHeight());
        }

        // Use a brush to re-enforce the land bridge area
        int mapSize = map.getSize();
        String brushName = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));
        landBridgeBrush.setSize(landNoiseMap.getSize())
                       .addBrush(new Vector2((float) mapSize / 2, (float) mapSize / 2), brushName, 1, 256,
                                 mapSize / 10);
        landNoiseMap.clampMin(landBridgeBrush, fractalParams.fractalFlattenParams().get(1).destinationMinHeight())
                    .blur(15, landBridgeBrush.copy().inflate(15));

        super.setupHeightmapPipeline();

        // Do some post-processing to make the islands in the water area a little higher
        // So that T1 navy can't shoot mexes on the islands
        BooleanMask landMask = heightmap.copyAsBooleanMask(
                waterHeight - fractalParams.waterHeight() + fractalParams.fractalFlattenParams()
                                                                         .get(0)
                                                                         .destinationMaxHeight());
        FloatMask islandElevatorMask =
                waterArea
                        .copy()
                        .deflate((int) (mapSize / 8f))
                        .copyAsFloatMask(0f, 4f)
                        .blur(mapSize / 16)
                        .setToValue(landMask.invert(), 0f)
                        .blur(2);

        heightmap.add(islandElevatorMask);
    }

    @Override
    protected int getMinTeammateSeparation() {
        int numTeams = generatorParameters.numTeams();
        if (numTeams == 0) {
            // Special rule for FFA, this is the largest separation that works
            return map.getSize() * 2 / generatorParameters.spawnCount();
        }

        // This spaces teammates as far as possible from each other.
        // On a 20k 4v4 teammates will be 128 apart, making for a better Setons game
        int spawnsPerTeam = generatorParameters.spawnCount() / numTeams;
        if (spawnsPerTeam <= 0) {
            spawnsPerTeam = 1;
        }
        return map.getSize() / 6 / spawnsPerTeam * 4;
    }

    @Override
    protected int getMaxTeammateSeparation() {
        return map.getSize() / 2;
    }

    @Override
    public int getTeamSeparation() {
        return map.getSize() / 3;
    }

    @Override
    protected void setupSpawnMaskPipeline() {
        for (FractalFlattenParams fractalFlattenParams : fractalParams.fractalFlattenParams()) {
            if (fractalFlattenParams.spawnable()) {
                spawnMask.add(landNoiseMap.copyAsBooleanMask(fractalFlattenParams.minHeight(),
                                                             fractalFlattenParams.maxHeight())
                                          .deflate(fractalFlattenParams.spawnMaskDeflate()));
            }
        }

        spawnMask.subtract(unbuildable)
                 .subtract(waterArea) // For Setons, subtract the water area to prevent spawning on the island
                 .fillCenter(map.getSize() / 3, false)
                 .deflate(fractalParams.spawnMaskDeflate());

        mexDeadZoneNoise.setSize(bridgeLandArea.getSize())
                        .addWhiteNoise(0, 1);
        mexDeadZone.add(bridgeLandArea.copy().subtract(mexDeadZoneNoise.copyAsBooleanMask(0.7f)));
    }
}
