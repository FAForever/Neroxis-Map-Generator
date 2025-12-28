package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.FractalWaterMasks;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.Pipeline;
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
                        new FractalFlattenParams(0f, 0.5f, 0, 8, 0.1f, 0, false, false, 4),
                        new FractalFlattenParams(0.5f, 3f, 8, 16, 4f, 0, true, false, 4),
                        new FractalFlattenParams(3f, 30, 16, 16, 2f, 1, false, true, 4),
                        new FractalFlattenParams(30, 50, 16, 24, 0.5f, 1,  false, false, 4)
                )
        );
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
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
                landNoiseMap.copyAsBooleanMask(fractalParams.fractalFlattenParams().get(3).minHeight(),
                                               fractalParams.fractalFlattenParams().get(3).maxHeight())
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
        landNoiseMap.setToMinValueForArea(landBridgeBrush, 16)
                .blur(15, landBridgeBrush.copy().inflate(15));

        super.setupHeightmapPipeline();
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


    }

    protected void setupPassablePipeline() {
        super.setupPassablePipeline();
        passableLand.subtract(bridgeLandArea);
    }
}
