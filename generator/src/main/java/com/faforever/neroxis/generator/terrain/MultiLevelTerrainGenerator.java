package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.vector.Vector3;

public class MultiLevelTerrainGenerator extends BasicTerrainGenerator {

    protected BooleanMask secondLevelLand;
    protected BooleanMask thirdLevelLand;

    protected FloatMask treeGroupDensityMap;


    protected float landNoiseMapFirstLevel;
    protected float landNoiseMapSecondLevel;
    protected float landNoiseMapThirdLevel;

    String[] SPAWN_MASK_BRUSHES = {
            "mountain4.png",
            "mountain6.png",
            "mountain7.png",
            "mountain8.png",
            "mountain9.png",
    };

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);

        secondLevelLand = new BooleanMask(1, random.nextLong(), symmetrySettings, "secondLevelLand", true);
        thirdLevelLand = new BooleanMask(1, random.nextLong(), symmetrySettings, "secondLevelLand", true);

        treeGroupDensityMap = new FloatMask(1, random.nextLong(), symmetrySettings, "treeGroupDensityMap", true);

        landNoiseMapFirstLevel = 0.385f;
        landNoiseMapSecondLevel = 0.52f;
        landNoiseMapThirdLevel = 0.61f;

        spawnSize = 48;

        plateauHeight = 6f;
        plateauBrushIntensity = 16f;

        mountainDensity = random.nextFloat() / 3;
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();

        int MAX_OCTAVES = 7;
        int numOctaves = 0;
        int smallestDetail = 5;

        while (numOctaves < MAX_OCTAVES && smallestDetail << numOctaves <= mapSize) {
            numOctaves++;
        }

        FloatMask landNoiseMap = new FloatMask(mapSize, getRandom().nextLong(), land.getSymmetrySettings(), "landNoiseMap", true);
        for (int octave = 0; octave < numOctaves; octave++) {
            FloatMask octaveNoise = new FloatMask(mapSize, getRandom().nextLong(), land.getSymmetrySettings(), "landNoiseOctave" + octave, true);
            octaveNoise.addPerlinNoise(smallestDetail << octave,1f / numOctaves);
            landNoiseMap.add(octaveNoise);
        }
        landNoiseMap.blur(8);

        float firstLevel = landNoiseMapFirstLevel;
        float secondLevel = random.nextFloat(landNoiseMapSecondLevel, landNoiseMapSecondLevel + 0.02f);
        float thirdLevel = random.nextFloat(landNoiseMapThirdLevel, landNoiseMapThirdLevel + 0.03f);

        land = landNoiseMap
                .copyAsBooleanMask(firstLevel)
                .erode(0.3f, 10)
                .setSize(mapSize+1);
        secondLevelLand = landNoiseMap
                .copyAsBooleanMask(secondLevel)
                .erode(0.3f, 10)
                .setSize(mapSize+1);
        thirdLevelLand = landNoiseMap
                .copyAsBooleanMask(thirdLevel)
                .erode(0.3f, 10)
                .setSize(mapSize+1);
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

        FloatMask rampExclusion = new FloatMask(ramps.getSize(), random.nextLong(), this.symmetrySettings, "rampExclusion", true);
        rampExclusion.addPerlinNoise(StrictMath.min(128, rampExclusion.getSize()), 1);
        BooleanMask rampExclusionMask = rampExclusion.copyAsBooleanMask(0.4f, 0.6f);

        ramps.subtract(rampExclusionMask.invert());
    }

    @Override
    protected void blurRamps() {
        BooleanMask inflatedRamps = ramps.copy();
        heightmap.blur(48, inflatedRamps)
                 .blur(32, inflatedRamps.inflate(2))
                 .blur(4, inflatedRamps.copy().outline().inflate(4))
                 .blur(6, inflatedRamps.copy().outline().inflate(6))
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

        heightmapLand.flattenSpawnPointsWithRadius(map.getSpawns(), "mountain4.png",spawnSize)
                     .blur(5, spawnLandMask);

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
