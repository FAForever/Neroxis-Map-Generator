package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.util.List;

public class MultiLevelTerrainGenerator extends BasicTerrainGenerator {

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

    protected Pipeline pipeline;

    String[] SPAWN_MASK_BRUSHES = {
            "mountain4.png",
            "mountain6.png",
            "mountain7.png",
            "mountain8.png",
            "mountain9.png",
    };

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
        this.pipeline = pipeline;
        landNoiseMap = new FloatMask(1, getRandom().nextLong(), land.getSymmetrySettings(), "landNoiseMap", pipeline);
        secondLevelLand = new BooleanMask(1, random.nextLong(), symmetrySettings, "secondLevelLand", pipeline);
        thirdLevelLand = new BooleanMask(1, random.nextLong(), symmetrySettings, "secondLevelLand", pipeline);

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
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();

        int MAX_OCTAVES = 8;
        int numOctaves = 0;

        while (numOctaves < MAX_OCTAVES && noiseSmallestDetail << numOctaves <= mapSize) {
            numOctaves++;
        }

        float amplitude = 1f;
        landNoiseMap.setSize(mapSize + 1);
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

    @Override
    protected void spawnTerrainSetup() {
        int mapSize = map.getSize();
        spawnPlateauMask.setSize(mapSize / 4);
        spawnPlateauMask.erode(.5f, 4).dilute(.5f, 8);
        spawnPlateauMask.erode(.5f).setSize(mapSize + 1);
        spawnPlateauMask.blur(4);

        spawnLandMask.setSize(mapSize / 4);
        spawnLandMask.erode(.25f, mapSize / 128).dilute(.5f, 4);
        spawnLandMask.erode(.5f).setSize(mapSize + 1);
        spawnLandMask.blur(4);

        plateaus.subtract(spawnLandMask).add(spawnPlateauMask);
        land.add(spawnLandMask).add(spawnPlateauMask);
        thirdLevelLand.subtract(spawnLandMask);

        mountains.subtract(spawnLandMask.copy().inflate(mountainBrushSize / 4f));

        plateaus.multiply(land).subtract(spawnLandMask).add(spawnPlateauMask);
        land.add(plateaus).add(spawnLandMask).add(spawnPlateauMask);
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

        FloatMask rampExclusion = new FloatMask(ramps.getSize(), random.nextLong(), this.symmetrySettings,
                                                "rampExclusion", pipeline);
        rampExclusion.addPerlinNoise(StrictMath.min(128, rampExclusion.getSize()), 1);
        BooleanMask rampExclusionMask = rampExclusion.copyAsBooleanMask(0.4f, 0.6f);

        ramps.subtract(rampExclusionMask.invert());
    }

    @Override
    protected void blurRamps() {
        BooleanMask inflatedRamps = ramps.copy().subtract(spawnLandMask);
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

    @Override
    protected void spawnMaskSetup() {
        map.getSpawns().forEach(spawn -> {
            Vector3 location = spawn.getPosition();
            spawnLandMask.fillCircle(location, spawnSize, true);
        });
    }


}
