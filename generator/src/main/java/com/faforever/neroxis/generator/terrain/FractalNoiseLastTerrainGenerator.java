package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.Set;

public class FractalNoiseLastTerrainGenerator extends MultiLevelLastTerrainGenerator {

    private BooleanMask symmetryLines;
    private FloatMask symmetryCliffs;
    private FloatMask rampNoise;

    protected FractalParams fractalParams;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);

        symmetryLines = new BooleanMask(1, random.nextLong(), symmetrySettings, "symmetryLines");
        symmetryCliffs = new FloatMask(1, random.nextLong(), symmetrySettings, "symmetryCliffs");
        rampNoise = new FloatMask(1, random.nextLong(), symmetrySettings, "rampNoise");

        if (fractalParams.useRandomWaterMask()) {
            switch (random.nextInt(3)) {
                case 0:
                    waterMask = WaterMasks.SYMMETRY_LINE;
                    break;
                case 1:
                    waterMask = WaterMasks.HOUR_GLASS;
                    break;
                case 2:
                    waterMask = WaterMasks.CENTER_LAKE;
                    break;
            }
        }

        noiseSmallestDetail = 2;
        noiseOctaveMultiplier = fractalParams.noiseOctaveMultiplier();

        noiseMapBlurAmount = fractalParams.noiseMapBlurAmount();
        noiseScaleMaxToValue = 50;

        mountainBrushSize = 24;
        mountainBrushDensity = 8f;
        mountainBrushIntensity = 3f;

        waterHeight -= fractalParams.waterHeight();

        symmetryLines.setSize(map.getSize() + 1);
        symmetryLines.drawSymmetryLines(symmetrySettings.terrainSymmetry());
    }

    @Override
    protected void landSetup() {
        super.landSetup();

        landNoiseMap.scaleToNewMinAndMaxHeight(0, 1);
        landNoiseMap.scaleExponentially(fractalParams.noiseExpMultiplier());
        landNoiseMap.scaleToNewMinAndMaxHeight(0, noiseScaleMaxToValue);
    }

    @Override
    protected void mountainSetup() {
        symmetryCliffs = landNoiseMap.copy();
        symmetryCliffs.supcomGradient();
        symmetryCliffs.setToValue(symmetryLines.copy().inflate(3).invert(), 0f);

        mountains.setSize(map.getSize() + 1).set((x, y) -> true);
    }

    @Override
    protected void setupMountainHeightmapPipeline() {
        heightmapMountains.setSize(map.getSize() + 1);
        heightmapMountains.useBrushWithCliffMap(symmetryCliffs, mountainBrushSize);
        heightmapMountains.scaleToNewMinAndMaxHeight(0, noiseScaleMaxToValue - 5f);
        heightmapMountains.set((x, y) -> heightmapMountains.get(x, y) <= 0 ? -128f : heightmapMountains.get(x, y));

        BooleanMask paintedMountains = heightmapMountains.copyAsBooleanMask(plateauHeight / 2);

        mountains.init(paintedMountains);
    }

    @Override
    protected void initRamps() {
        ramps.setSize(landNoiseMap.getSize());

        for (FractalFlattenParams fractalFlattenParams : fractalParams.fractalFlattenParams()) {
            if (fractalFlattenParams.hasRamps()) {
                BooleanMask layer = landNoiseMap.copyAsBooleanMask(0f, fractalFlattenParams.maxHeight());
                layer.outline();

                rampNoise.setSize(landNoiseMap.getSize() / 16);
                rampNoise.addWhiteNoise(0, 1);
                rampNoise.setSize(landNoiseMap.getSize());

                layer.subtract(rampNoise.copyAsBooleanMask(0f, 0.8f));

                ramps.add(layer);
            }
        }
    }

    @Override
    protected void blurRamps() {
        BooleanMask inflatedRamps = ramps.copy();
        heightmap.blur(48, inflatedRamps)
                 .blur(32, inflatedRamps.inflate(2))
                 .blur(4, inflatedRamps.inflate(4))
                 .blur(4, inflatedRamps.inflate(8))
                 .clampMin(0f)
                 .clampMax(255f);
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

        for (FractalFlattenParams fractalFlattenParams : fractalParams.fractalFlattenParams()) {
            MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, fractalFlattenParams.minHeight(),
                                             fractalFlattenParams.maxHeight(),
                                             fractalFlattenParams.destinationMinHeight(),
                                             fractalFlattenParams.destinationMaxHeight(),
                                             fractalFlattenParams.slope(), fractalFlattenParams.blurAmount());
        }


        // Blur and add mountains along the line of symmetry
        if (Set.of(Symmetry.POINT2, Symmetry.POINT3, Symmetry.POINT4, Symmetry.POINT5,
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

        if (!symmetrySettings.spawnSymmetry().isPerfectSymmetry()) {
            // For the odd symmetry, pie shaped maps, we need to limit the terrain to a circle with the full diameter of the map
            BooleanMask outerCircle = new BooleanMask(mapSize + 1, random.nextLong(), symmetrySettings, "outerCircle");
            outerCircle.fillCircle(new Vector2(mapSize / 2f, mapSize / 2f), mapSize / 2f, true);
            outerCircle.invert();
            heightmap.setToValue(outerCircle, waterHeight);
            heightmap.blur(5, outerCircle.outline().inflate(5));
        }

        blurRamps();
    }

    @Override
    protected void setupSpawnMaskPipeline() {
        for (FractalFlattenParams fractalFlattenParams : fractalParams.fractalFlattenParams()) {
            if (fractalFlattenParams.spawnable()) {
                spawnMask.add(landNoiseMap.copyAsBooleanMask(fractalFlattenParams.minHeight(),
                                                             fractalFlattenParams.maxHeight())
                                          .deflate((int) fractalFlattenParams.spawnMaskDeflate()));
            }
        }

        spawnMask.subtract(unbuildable)
                 .fillCenter(map.getSize() / 3, false)
                 .deflate(4);
    }

    @Override
    public int getTeamSeparation() {
        if (generatorParameters.numTeams() < 2) {
            return 0;
        } else if (generatorParameters.numTeams() == 2) {
            return map.getSize() / fractalParams.teamSeparation();
        } else {
            return StrictMath.min(map.getSize() / generatorParameters.numTeams(), 256);
        }
    }
}
