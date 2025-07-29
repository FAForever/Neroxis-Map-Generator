package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;

public class FractalNoiseTerrainGenerator extends MultiLevelTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);

        noiseSmallestDetail = 2;

        noiseOctaveMultiplier = 1.5f;

        noiseMapBlurAmount = 1;
        noiseScaleMaxToValue = 50;

        waterHeight -= landHeight -1;
    }

    @Override
    protected void initRamps() {
    }

    @Override
    protected void blurRamps() {
    }

    @Override
    protected void setupHeightmapPipeline() {
        int mapSize = map.getSize();
        int numBrushes = Brushes.GENERATOR_BRUSHES.size();

        String brush = Brushes.GENERATOR_BRUSHES.get(random.nextInt(numBrushes));

        setupMountainHeightmapPipeline();
        setupPlateauHeightmapPipeline();
        setupSmallFeatureHeightmapPipeline();
        initRamps();

        BooleanMask water = land.copy().invert();
        BooleanMask deepWater = water.copy().deflate(32);

        heightmap.setSize(mapSize + 1).startVisualDebugger();
        heightmapLand.setSize(mapSize + 1).startVisualDebugger();
        heightmapOcean.setSize(mapSize + 1);
        heightMapNoise.setSize(mapSize / 128);

        heightmapOcean.addDistance(land, -.45f)
                      .clampMin(oceanFloor)
                      .useBrushWithinAreaWithDensity(water.deflate(8).subtract(deepWater), brush, shallowWaterBrushSize,
                                                     shallowWaterBrushDensity, shallowWaterBrushIntensity, false)
                      .useBrushWithinAreaWithDensity(deepWater, brush, deepWaterBrushSize, deepWaterBrushDensity,
                                                     deepWaterBrushIntensity, false)
                      .clampMax(0f)
                      .blur(4, deepWater)
                      .blur(1);


        landNoiseMap.scaleToNewMinAndMaxHeight(0, 1);
        landNoiseMap.scaleExponentially(6);
        landNoiseMap.scaleToNewMinAndMaxHeight(0, noiseScaleMaxToValue);
        heightmapLand.add(landNoiseMap);



        // Main land part of the map
        MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, 1, 4, 1, 0);

        // Plateua
        MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, 4, 6, 13, 2);
        MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, 6, 15, 11, 0);
        //heightmapLand.blur(1);

//        MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, 15, 17, 14);
//        MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, 17, 22, 13);
        MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, 15, 22, 16, 1);
        //MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, 17, 22, 13);

        //heightmap.blur(1);

//        heightmapLand.add(heightmapHills)
//                     .add(heightmapValleys)
//                     .add(heightmapMountains);


        heightmapLand.add(heightmapPlateaus)
                     .setToValue(spawnPlateauMask, plateauHeight + landHeight)
                     .blur(1, spawnLandMask.copy().inflate(4))
                     .blur(1, spawnPlateauMask.copy().inflate(4))
                     .add(heightmapOcean);

        List<Vector2> team0Spawns = map.getSpawns()
                                       .stream()
                                       .filter(spawn -> spawn.getTeamID() == 0)
                                       .map(Spawn::getPosition)
                                       .map(Vector2::new)
                                       .toList();
        MapMaskMethods.flattenPointsWithRadius(team0Spawns, heightmapLand, "mountain4.png", spawnSize);

        heightmap.add(heightmapLand)
                 .add(waterHeight);

        if (heightMapNoise.getSymmetrySettings().spawnSymmetry().isPerfectSymmetry()) {
            heightMapNoise.addWhiteNoise(plateauHeight / 3).resample(mapSize / 64);
            heightMapNoise.addWhiteNoise(plateauHeight / 3).resample(mapSize + 1);
            heightMapNoise.addWhiteNoise(1)
                          .subtractAvg()
                          .clampMin(0f)
                          .setToValue(land.copy().invert().inflate(16), 0f)
                          .blur(mapSize / 16, spawnLandMask.copy().inflate(8))
                          .blur(mapSize / 16, spawnPlateauMask.copy().inflate(8))
                          .blur(mapSize / 16);
            heightmap.add(heightMapNoise);
        }

        blurRamps();
    }
}
