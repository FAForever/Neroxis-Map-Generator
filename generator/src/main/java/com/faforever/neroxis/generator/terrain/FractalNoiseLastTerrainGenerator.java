package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.util.Pipeline;

import java.util.stream.Stream;

public class FractalNoiseLastTerrainGenerator extends MultiLevelLastTerrainGenerator {

    private BooleanMask symmetryLines;
    private FloatMask symmetryCliffs;
    private FloatMask noise;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);

        symmetryLines = new BooleanMask(1, random.nextLong(), symmetrySettings,"symmetryLines", pipeline);
        symmetryCliffs = new FloatMask(1, random.nextLong(), symmetrySettings, "symmetryCliffs", pipeline);
        noise = new FloatMask(1, random.nextLong(), symmetrySettings, "rampNoise", pipeline);

        noiseSmallestDetail = 2;

        noiseOctaveMultiplier = 1.5f;

        noiseMapBlurAmount = 1;
        noiseScaleMaxToValue = 50;

        mountainBrushSize = 24;
        mountainBrushDensity = 8f;
        mountainBrushIntensity = 3f;

        waterHeight -= landHeight -1;

        symmetryLines.setSize(map.getSize() + 1);
        symmetryLines.drawSymmetryLines();
    }

    @Override
    protected void landSetup() {
        super.landSetup();

        landNoiseMap.scaleToNewMinAndMaxHeight(0, 1);
        landNoiseMap.scaleExponentially(8);
        landNoiseMap.scaleToNewMinAndMaxHeight(0, noiseScaleMaxToValue);
    }

    @Override
    protected void mountainSetup() {
        symmetryCliffs = landNoiseMap.copy();
        symmetryCliffs.setVisualName("Symmetry Cliffs: ");
        symmetryCliffs.supcomGradient();
        symmetryCliffs.setToValue(symmetryLines.copy().inflate(3).invert(), 0f);

        mountains.setSize(map.getSize() + 1).set((x,y) -> true);
    }

    @Override
    protected void setupMountainHeightmapPipeline() {
        heightmapMountains.setSize(map.getSize() + 1);
        heightmapMountains.useBrushWithCliffMap(symmetryCliffs, mountainBrushSize);
        heightmapMountains.scaleToNewMinAndMaxHeight(0,noiseScaleMaxToValue - 5f);
        heightmapMountains.set((x, y) -> heightmapMountains.get(x, y) <= 0 ? -128f : heightmapMountains.get(x, y));

        BooleanMask paintedMountains = heightmapMountains.copyAsBooleanMask(plateauHeight / 2);

        mountains.init(paintedMountains);
    }

    @Override
    protected void initRamps() {
        ramps = landNoiseMap.copyAsBooleanMask(4f, 6f);
        noise.setSize(landNoiseMap.getSize() / 16);
        noise.addWhiteNoise(0, 1);
        noise.setSize(landNoiseMap.getSize());
        BooleanMask noiseMask = noise.copyAsBooleanMask(0f, 0.05f);
        noiseMask.inflate(4);
        ramps.subtract(noiseMask.invert());
        ramps.erode(3).inflate(3);
    }


    @Override
    protected void setupHeightmapPipeline() {
        int mapSize = map.getSize();

        initRamps();

        heightmap.setSize(mapSize + 1);
        heightmapLand.setSize(mapSize + 1);
        heightMapNoise.setSize(mapSize / 128);

        // Start the land height as the noise map
        heightmapLand.add(landNoiseMap);

        // Main land part of the map
        MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, 1, 4, 1, 0);

        // Plateau
        MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, 4, 6, 13, 2);
        MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, 6, 15, 11, 0);

        // 2nd Plateau, surrounding the mountains
        MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, 15, 22, 16, 1);

        // Blur and add mountains along the line of symmetry
        if (Set.of(Symmetry.QUAD, Symmetry.DIAG, Symmetry.POINT2, Symmetry.POINT3, Symmetry.POINT4, Symmetry.POINT5,
                      Symmetry.POINT6, Symmetry.POINT7, Symmetry.POINT8, Symmetry.POINT9, Symmetry.POINT10,
                      Symmetry.POINT11, Symmetry.POINT12, Symmetry.POINT13, Symmetry.POINT14, Symmetry.POINT15,
                      Symmetry.POINT16).contains(symmetrySettings.terrainSymmetry())
        ) {
            setupMountainHeightmapPipeline();
            heightmapLand.blur(3, symmetryLines.copy().inflate(10));
            heightmap.add(heightmapLand)
                     .max(heightmapMountains);
            heightmap.blur(1, symmetryLines.copy().inflate(10));
        } else {
            heightmap.add(heightmapLand);
        }
        heightmap.add(waterHeight);

        if (heightMapNoise.getSymmetrySettings().spawnSymmetry().isPerfectSymmetry()) {
            heightMapNoise.addWhiteNoise(plateauHeight / 3).resample(mapSize / 64);
            heightMapNoise.addWhiteNoise(plateauHeight / 3).resample(mapSize + 1);
            heightMapNoise.addWhiteNoise(1)
                          .subtractAvg()
                          .clampMin(0f)
                          .setToValue(land.copy().invert().inflate(16), 0f)
                          .blur(mapSize / 16);
            heightmap.add(heightMapNoise);
        }

        blurRamps();
    }
}
