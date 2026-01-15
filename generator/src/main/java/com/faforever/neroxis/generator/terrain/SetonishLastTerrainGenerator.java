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
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.Vertex;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;

public class SetonishLastTerrainGenerator extends FractalNoiseLastTerrainGenerator {
    BooleanMask landBridgeBrush;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        landBridgeBrush = new BooleanMask(1, seed, symmetrySettings, "mapWithBridge", pipeline);

        fractalParams = new FractalParams(
                15, FractalWaterMasks.SETONS, 2, 1.5f, 5, 2, 8, 50,
                List.of(
                        new FractalFlattenParams(0.0f, 3f, 6, 16, 3f, 0, true, false, 4),
                        new FractalFlattenParams(3f, 30, 16, 16, 2f, 1, false, true, 4),
                        new FractalFlattenParams(30, 50, 16, 24, 0.5f, 1,  false, false, 4)
                )
        );
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
    }

    @Override
    protected void addWaterAreasToNoiseMap(int mapSize) {
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
                waterArea.fillRect(halfMapSize - landRectanglePadding, halfMapSize - landRectanglePadding, landRectangleWidthAndHeight, landRectangleWidthAndHeight, true);
                bridgeLandArea.fillQuadrilateral(new Vertex(halfMapSize - (bridgeSize / 2) - landRectanglePadding, halfMapSize + landRectanglePadding),
                                                 new Vertex(halfMapSize + landRectanglePadding, halfMapSize - (bridgeSize / 2) - landRectanglePadding),
                                                 new Vertex(halfMapSize + (bridgeSize / 2) + landRectanglePadding, halfMapSize - landRectanglePadding),
                                                 new Vertex(halfMapSize - landRectanglePadding, halfMapSize + (bridgeSize / 2) + landRectanglePadding),
                                                 true);
                island = new Vector2((float) islandSize / 2, random.nextFloat(0, ((float) halfMapSize) - islandPadding));
            }
            case ZX -> {
                waterArea.fillRect(halfMapSize - landRectanglePadding, 0, landRectangleWidthAndHeight, landRectangleWidthAndHeight, true);
                waterArea.fillRect(0,  halfMapSize - landRectanglePadding, landRectangleWidthAndHeight, landRectangleWidthAndHeight, true);
                bridgeLandArea.fillQuadrilateral(new Vertex(halfMapSize - (bridgeSize / 2) - landRectanglePadding, halfMapSize - landRectanglePadding),
                                                 new Vertex(halfMapSize - landRectanglePadding, halfMapSize - (bridgeSize / 2) - landRectanglePadding),
                                                 new Vertex(halfMapSize + (bridgeSize / 2) + landRectanglePadding, halfMapSize + landRectanglePadding),
                                                 new Vertex(halfMapSize + landRectanglePadding, halfMapSize + (bridgeSize / 2) + landRectanglePadding),
                                                 true);
                island = new Vector2((float) islandSize / 2, random.nextFloat((float) halfMapSize + islandPadding, mapSize));
            }
            case X -> {
                waterArea.fillTriangle(List.of(
                                               new Vertex(-landRectanglePadding, 0),
                                               new Vertex(mapSize + landRectanglePadding, 0),
                                               new Vertex(halfMapSize, halfMapSize)
                                       ),
                                       true);
                waterArea.fillTriangle(List.of(
                                               new Vertex(-landRectanglePadding, mapSize),
                                               new Vertex(mapSize + landRectanglePadding, mapSize),
                                               new Vertex(halfMapSize, halfMapSize)
                                       ),
                                       true);
                bridgeLandArea.fillRect(halfMapSize - (bridgeSize / 2) - landRectanglePadding, halfMapSize - (bridgeSize / 2), bridgeSize + (landRectanglePadding * 2), bridgeSize, true);
                island = new Vector2(random.nextFloat(islandPadding, mapSize - islandPadding), (float) islandSize / 2);
            }
            case Z -> {
                waterArea.fillTriangle(List.of(
                                               new Vertex(0, -landRectanglePadding),
                                               new Vertex(0, mapSize + landRectanglePadding),
                                               new Vertex(halfMapSize, halfMapSize)
                                       ),
                                       true);
                waterArea.fillTriangle(List.of(
                                               new Vertex(mapSize, -landRectanglePadding),
                                               new Vertex(mapSize, mapSize + landRectanglePadding),
                                               new Vertex(halfMapSize, halfMapSize)
                                       ),
                                       true);
                bridgeLandArea.fillRect(halfMapSize - (bridgeSize / 2), halfMapSize - (bridgeSize / 2) - landRectanglePadding, bridgeSize, bridgeSize + (landRectanglePadding * 2), true);
                island = new Vector2((float) islandSize / 2, random.nextFloat(islandPadding, mapSize - islandPadding));
            }
            case NONE -> {
                // lets do nothing
            }
            default -> {
                waterArea.drawSymmetryLines(symmetrySettings.teamSymmetry());
                waterArea.inflate(mapSize / 4f / symmetrySettings.teamSymmetry().getNumSymPoints());
                bridgeLandArea.fillCircle(new Vector2((float) halfMapSize, (float) halfMapSize),mapSize / 10f, true);
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

        waterAreaBlur = waterAreaMinusIsland.copyAsFloatMask(0f, 1.8f);
        waterAreaBlur.blur(mapSize / 16);

        landNoiseMap.add(1f);
        landNoiseMap.subtract(waterAreaBlur);
    }

    @Override
    protected void setupMountainHeightmapPipeline() {
        // Draw some mountains specially implemented for Setons
        rawMountains.setSize(map.getSize() + 1);
        String brushName = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));

        BooleanMask avoidMountainMask = waterArea.copy().setSize(map.getSize()).deflate(50).setSize(map.getSize()+1);
        float densityMultiplier = 1f / (1024f / map.getSize());

        // Mountains within the main land area
        rawMountains.useBrushWithinAreaWithDensity(
                landNoiseMap.copyAsBooleanMask(fractalParams.fractalFlattenParams().get(2).minHeight(),
                                               fractalParams.fractalFlattenParams().get(2).maxHeight())
                            .subtract(avoidMountainMask)
                , brushName, 50, 2 * densityMultiplier, 0.75f, false);
    }

    @Override
    protected void setupHeightmapPipeline() {
        // This extra step in the heightmap pipeline creates islands in the water area
        // It raises the underwater mountains to be above water
        if (waterMask != FractalWaterMasks.NONE) {
            landNoiseMap.multiply(waterAreaBlur.copy().add(1f).scaleExponentially(1.3f));
            landNoiseMap.clampMax(fractalParams.clampMapHeight());
        }

        // Use a brush to re-enforce the land bridge area
        int mapSize = map.getSize();
        String brushName = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));
        landBridgeBrush.setSize(landNoiseMap.getSize())
                       .addBrush(new Vector2((float) mapSize / 2, (float) mapSize / 2), brushName, 1, 256, mapSize / 10);
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
                        .deflate(mapSize / 8f)
                        .copyAsFloatMask(0f, 4f)
                        .blur(mapSize / 16)
                        .setToValue(landMask.invert(), 0f)
                        .blur(2);

        heightmap.add(islandElevatorMask);
    }

    @Override
    protected int getTeammateSeparation() {
        // This spaces teammates as far as possible from each other.
        // On a 20k 4v4 teammates will be 128 apart, making for a better Setons game
        int numTeams = generatorParameters.numTeams();
        int spawnsPerTeam = numTeams > 0 ? generatorParameters.spawnCount() / numTeams : 1;
        if (spawnsPerTeam <= 0) {
            spawnsPerTeam = 1;
        }
        return map.getSize() / 6 / spawnsPerTeam * 4;
    }

    @Override
    protected int getTeamSeparation() {
        return map.getSize() / 3;
    }

    @Override
    protected void setupSpawnMaskPipeline() {
        for (FractalFlattenParams fractalFlattenParams : fractalParams.fractalFlattenParams()) {
            if (fractalFlattenParams.spawnable()) {
                spawnMask.add(landNoiseMap.copyAsBooleanMask(fractalFlattenParams.minHeight(), fractalFlattenParams.maxHeight())
                                          .deflate(fractalFlattenParams.spawnMaskDeflate()));
            }
        }

        spawnMask.subtract(unbuildable)
                 .subtract(waterArea) // For Setons, subtract the water area to prevent spawning on the island
                 .fillCenter(map.getSize() / 3, false)
                 .deflate(fractalParams.spawnMaskDeflate());


        mexDeadZone.add(bridgeLandArea.copy().subtract(rampNoise.copyAsBooleanMask(0.7f)));
    }
}
