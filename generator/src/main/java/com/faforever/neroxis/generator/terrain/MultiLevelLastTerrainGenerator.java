package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.generator.FractalWaterMasks;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;

public class MultiLevelLastTerrainGenerator extends BasicLastTerrainGenerator {

    protected FractalWaterMasks waterMask;
    protected BooleanMask waterArea;

    protected BooleanMask secondLevelLand;
    protected BooleanMask thirdLevelLand;

    protected FloatMask landNoiseMap;
    protected int noiseSmallestDetail;
    protected float noiseOctaveMultiplier;
    protected int noiseMapBlurAmount;

    protected int noiseScaleMaxToValue;
    protected float landNoiseMapFirstLevel;
    protected float landNoiseMapSecondLevel;
    protected float landNoiseMapThirdLevel;
    protected FloatMask waterAreaBlur;

    protected FloatMask rampExclusion;

    protected Pipeline pipeline;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
        this.pipeline = pipeline;
        landNoiseMap = new FloatMask(1, getRandom().nextLong(), land.getSymmetrySettings(), "landNoiseMap", pipeline);
        secondLevelLand = new BooleanMask(1, random.nextLong(), symmetrySettings, "secondLevelLand", pipeline);
        thirdLevelLand = new BooleanMask(1, random.nextLong(), symmetrySettings, "thirdLevelLand", pipeline);
        rampExclusion = new FloatMask(1, random.nextLong(), symmetrySettings, "rampExclusion", pipeline);
        waterAreaBlur = new FloatMask(1, random.nextLong(), symmetrySettings, "waterAreaBlur", pipeline);
        waterArea = new BooleanMask(1, random.nextLong(), symmetrySettings, "waterArea", pipeline);

        noiseSmallestDetail = 5;
        noiseOctaveMultiplier = 1.0f;
        noiseMapBlurAmount = 8;

        noiseScaleMaxToValue = 40;
        landNoiseMapFirstLevel = 6;
        landNoiseMapSecondLevel = 25;
        landNoiseMapThirdLevel = 35;

        spawnSize = 64;

        plateauHeight = 6f;
        plateauBrushIntensity = 16f;

        mountainDensity = random.nextFloat() / 3;
        waterMask = FractalWaterMasks.NONE;
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();

        int MAX_OCTAVES = 7;
        int numOctaves = 0;

        while (numOctaves < MAX_OCTAVES && noiseSmallestDetail << numOctaves <= mapSize) {
            numOctaves++;
        }

        float amplitude = 1f;
        landNoiseMap.setSize(mapSize + 1);

        if (waterMask != FractalWaterMasks.NONE) {
            addWaterAreasToNoiseMap(mapSize);
        }

        for (int octave = 0; octave < numOctaves; octave++) {
            FloatMask octaveNoise = new FloatMask(mapSize + 1, getRandom().nextLong(), land.getSymmetrySettings(),
                                                  "landNoiseOctave" + octave, pipeline);
            octaveNoise.addPerlinNoise(noiseSmallestDetail << octave, 1f / numOctaves);
            octaveNoise.multiply(amplitude);
            landNoiseMap.add(octaveNoise);
            amplitude *= noiseOctaveMultiplier;
        }
        landNoiseMap.blur(noiseMapBlurAmount);


        landNoiseMap.scaleToNewMinAndMaxHeight(0, noiseScaleMaxToValue);

        land = landNoiseMap
                .copyAsBooleanMask(landNoiseMapFirstLevel)
                .erode(0.3f, 10);
        secondLevelLand = landNoiseMap
                .copyAsBooleanMask(landNoiseMapSecondLevel)
                .erode(0.3f, 10);
        thirdLevelLand = landNoiseMap
                .copyAsBooleanMask(landNoiseMapThirdLevel)
                .erode(0.3f, 10);
    }

    private void addWaterAreasToNoiseMap(int mapSize) {
        // For water only, we can influence the likelihood of water to occur for different areas of the map
        waterArea.setSize(mapSize + 1);

        switch (waterMask) {
            case FractalWaterMasks.SYMMETRY_LINE -> {
                // Water will be more likely along the symmetry line(s), kinda splitting the map in half, or pie slices for odd symmetries
                waterArea.drawSymmetryLines(symmetrySettings.terrainSymmetry());
                waterArea.inflate(StrictMath.min(256, mapSize / 4f / symmetrySettings.teamSymmetry().getNumSymPoints()));
                waterAreaBlur = waterArea.copyAsFloatMask(0f, 1f);
                waterAreaBlur.blur(mapSize / 3 / symmetrySettings.teamSymmetry().getNumSymPoints());
            }
            case FractalWaterMasks.HOUR_GLASS -> {
                // An unusual shape, which increase the likelihood of water along the symmetry lines and the corners of the map
                waterArea.drawSymmetryLines(symmetrySettings.terrainSymmetry());
                waterArea.inflate(mapSize / 4f / symmetrySettings.teamSymmetry().getNumSymPoints());
                List<Vector2> symmetryPoints = waterArea.getSymmetryPointsWithOutOfBounds(new Vector2(0, 0),
                                                                                          SymmetryType.SPAWN)
                                                        .stream()
                                                        .map(Vector2::roundToNearestHalfPoint)
                                                        .toList();
                waterArea.fillCircle(new Vector2(0, 0),mapSize / 2f / symmetrySettings.teamSymmetry().getNumSymPoints(), true);
                symmetryPoints.forEach(
                        s -> waterArea.fillCircle(s, mapSize / 2f / symmetrySettings.teamSymmetry().getNumSymPoints(),
                                                  true));
                waterAreaBlur = waterArea.copyAsFloatMask(0f, 1f);
                waterAreaBlur.blur(mapSize / 3 / symmetrySettings.teamSymmetry().getNumSymPoints());
            }
            case FractalWaterMasks.CENTER_LAKE -> {
                // big ocean in the centre of the map
                waterArea.fillCircle(new Vector2(mapSize / 2f, mapSize / 2f), mapSize / 3f, true);
                waterAreaBlur = waterArea.copyAsFloatMask(0f, 1f);
                waterAreaBlur.blur(mapSize / 8);
            }
            case FractalWaterMasks.SETONS -> {
                int padding = mapSize / 64;

                switch (symmetrySettings.teamSymmetry()) {
                    case DIAG, XZ -> {
                        waterArea.fillRect(0, 0, (mapSize / 2) - padding, (mapSize / 2) - padding, true);
                        waterArea.fillRect((mapSize / 2) + padding, (mapSize / 2) + padding, mapSize, mapSize, true);
                    }
                    case ZX -> {
                        waterArea.fillRect((mapSize / 2) + padding, 0, (mapSize / 2) + padding, (mapSize / 2) - padding, true);
                        waterArea.fillRect(0,  (mapSize / 2) + padding, (mapSize / 2) - padding, mapSize, true);
                    }
                    case X -> {
                        waterArea.fillTriangle(0, 0, mapSize, 0, mapSize / 2, mapSize / 2, true);
                        waterArea.fillTriangle(0, mapSize, mapSize, mapSize, mapSize / 2, mapSize / 2, true);
                    }
                    case POINT2, Z -> {
                        waterArea.fillTriangle(0, 0, 0, mapSize, mapSize / 2, mapSize / 2, true);
                        waterArea.fillTriangle(mapSize, 0, mapSize, mapSize, mapSize / 2, mapSize / 2, true);
                    }
                    case NONE -> {
                        // lets do nothing
                    }
                    default -> {
                        waterArea.drawSymmetryLines(symmetrySettings.teamSymmetry());
                        waterArea.inflate(mapSize / 4f / symmetrySettings.teamSymmetry().getNumSymPoints());
                    }
                }

                // Remove a circle from the middle of the water map
                waterArea.fillCircle(new Vector2((float) mapSize / 2, (float) mapSize / 2),
                                     mapSize / 10f, false);


                waterAreaBlur = waterArea.copyAsFloatMask(0f, 1.8f);
                waterAreaBlur.blur(mapSize / 16);
            }
        }

        landNoiseMap.add(1f);
        landNoiseMap.subtract(waterAreaBlur);
    }

    @Override
    protected void plateausSetup() {
        int mapSize = map.getSize();
        plateaus.setSize(mapSize + 1);
    }

    @Override
    protected void initRamps() {
        ramps.setSize(map.getSize() + 1);

        ramps = secondLevelLand.copy();
        ramps.outline();

        rampExclusion.setSize(ramps.getSize());
        rampExclusion.addPerlinNoise(StrictMath.min(128, rampExclusion.getSize()), 1);
        BooleanMask rampExclusionMask = rampExclusion.copyAsBooleanMask(0.4f, 0.6f);

        ramps.subtract(rampExclusionMask.invert());
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
    protected void setupSpawnMaskPipeline() {
        spawnMask.init(land)
                 .subtract(secondLevelLand)
                 .subtract(unbuildable)
                 .deflate(8);
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

        heightmap.setSize(mapSize + 1);
        heightmapLand.setSize(mapSize + 1);
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

        heightmapLand.add(heightmapHills)
                     .add(heightmapValleys)
                     .add(heightmapMountains)
                     .add(landHeight)
                     .add(secondLevelLand, plateauHeight)
                     .blur(1, secondLevelLand.copy().inflate(6))
                     .add(thirdLevelLand, plateauHeight)
                     .blur(1, thirdLevelLand.copy().inflate(6));


        heightmapLand.add(heightmapPlateaus)
                     .add(heightmapOcean);

        heightmap.add(heightmapLand)
                 .add(waterHeight);

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
