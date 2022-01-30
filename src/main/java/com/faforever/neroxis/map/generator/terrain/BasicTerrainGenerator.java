package com.faforever.neroxis.map.generator.terrain;

import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.vector.Vector3;

public strictfp class BasicTerrainGenerator extends TerrainGenerator {
    protected BooleanMask spawnLandMask;
    protected BooleanMask spawnPlateauMask;
    protected BooleanMask land;
    protected BooleanMask mountains;
    protected BooleanMask hills;
    protected BooleanMask valleys;
    protected BooleanMask plateaus;
    protected BooleanMask ramps;
    protected BooleanMask connections;
    protected FloatMask heightmapValleys;
    protected FloatMask heightmapHills;
    protected FloatMask heightmapPlateaus;
    protected FloatMask heightmapMountains;
    protected FloatMask heightmapLand;
    protected FloatMask heightmapOcean;
    protected FloatMask heightMapNoise;

    protected int spawnSize;
    protected float waterHeight;
    protected float plateauHeight;
    protected float oceanFloor;
    protected float valleyFloor;
    protected float landHeight;
    protected float shallowWaterBrushIntensity;
    protected float deepWaterBrushIntensity;
    protected float plateauBrushDensity;
    protected float valleyBrushDensity;
    protected float hillBrushDensity;
    protected int shallowWaterBrushSize;
    protected float shallowWaterBrushDensity;
    protected int deepWaterBrushSize;
    protected float deepWaterBrushDensity;
    protected int mountainBrushSize;
    protected int plateauBrushSize;
    protected int smallFeatureBrushSize;
    protected float mountainBrushIntensity;
    protected float plateauBrushIntensity;
    protected float valleyBrushIntensity;
    protected float hillBrushIntensity;
    protected float mountainBrushDensity;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        spawnLandMask = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "spawnLandMask", true);
        spawnPlateauMask = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "spawnPlateauMask", true);
        land = new BooleanMask(1, random.nextLong(), symmetrySettings, "land", true);
        mountains = new BooleanMask(1, random.nextLong(), symmetrySettings, "mountains", true);
        plateaus = new BooleanMask(1, random.nextLong(), symmetrySettings, "plateaus", true);
        ramps = new BooleanMask(1, random.nextLong(), symmetrySettings, "ramps", true);
        hills = new BooleanMask(1, random.nextLong(), symmetrySettings, "hills", true);
        valleys = new BooleanMask(1, random.nextLong(), symmetrySettings, "valleys", true);
        connections = new BooleanMask(1, random.nextLong(), symmetrySettings, "connections", true);
        heightmapValleys = new FloatMask(1, random.nextLong(), symmetrySettings, "heightmapValleys", true);
        heightmapHills = new FloatMask(1, random.nextLong(), symmetrySettings, "heightmapHills", true);
        heightmapPlateaus = new FloatMask(1, random.nextLong(), symmetrySettings, "heightmapPlateaus", true);
        heightmapMountains = new FloatMask(1, random.nextLong(), symmetrySettings, "heightmapMountains", true);
        heightmapLand = new FloatMask(1, random.nextLong(), symmetrySettings, "heightmapLand", true);
        heightmapOcean = new FloatMask(1, random.nextLong(), symmetrySettings, "heightmapOcean", true);
        heightMapNoise = new FloatMask(1, random.nextLong(), symmetrySettings, "noise", true);

        spawnSize = 48;
        waterHeight = mapParameters.getBiome().getWaterSettings().getElevation();
        plateauHeight = 6f;
        oceanFloor = -16f;
        valleyFloor = -5f;
        landHeight = .25f;

        mountainBrushSize = map.getSize() < 512 ? 32 : 64;
        mountainBrushDensity = map.getSize() < 512 ? .1f : .05f;
        mountainBrushIntensity = 10f;

        plateauBrushSize = 64;
        plateauBrushDensity = .16f;
        plateauBrushIntensity = 10f;

        smallFeatureBrushSize = 24;
        hillBrushIntensity = 0.5f;
        valleyBrushDensity = .72f;
        hillBrushDensity = .72f;

        shallowWaterBrushIntensity = .5f;
        shallowWaterBrushSize = 24;
        shallowWaterBrushDensity = 1f;
        deepWaterBrushIntensity = 1f;
        deepWaterBrushSize = 64;
        deepWaterBrushDensity = .065f;
    }

    @Override
    protected void terrainSetup() {
        spawnMaskSetup();
        teamConnectionsSetup();
        landSetup();
        plateausSetup();
        mountainSetup();
        symmetrySetup();
        spawnTerrainSetup();
        enforceSymmetry();
        setupHeightmapPipeline();
    }

    protected void symmetrySetup() {
        if (!mapParameters.getSymmetrySettings().getSpawnSymmetry().isPerfectSymmetry()) {
            float halfSize = map.getSize() / 2f;
            int forceRadius = mapParameters.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();
            land.limitToCenteredCircle(halfSize).forceSymmetry().inflate(forceRadius).deflate(forceRadius);
            plateaus.limitToCenteredCircle(halfSize).forceSymmetry().inflate(forceRadius).deflate(forceRadius);
            mountains.limitToCenteredCircle(halfSize).forceSymmetry().inflate(forceRadius).deflate(forceRadius);
        }
    }

    protected void spawnMaskSetup() {
        map.getSpawns().forEach(spawn -> {
            Vector3 location = spawn.getPosition();
            spawnLandMask.fillCircle(location, spawnSize, true);
        });

        if (random.nextFloat() < mapParameters.getPlateauDensity()) {
            map.getSpawns().forEach(spawn -> {
                Vector3 location = spawn.getPosition();
                spawnPlateauMask.fillCircle(location, spawnSize, true);
            });
        }
    }

    protected void teamConnectionsSetup() {
        float maxStepSize = map.getSize() / 128f;
        int minMiddlePoints = 0;
        int maxMiddlePoints = 1;
        int numTeamConnections = (int) ((mapParameters.getRampDensity() + mapParameters.getPlateauDensity() + mapParameters.getMountainDensity()) / 3 * 2 + 1);
        int numTeammateConnections = 1;
        connections.setSize(map.getSize() + 1);

        connectTeamsAroundCenter(connections, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize, 32);
        connectTeammates(connections, maxMiddlePoints, numTeammateConnections, maxStepSize);
    }

    protected void landSetup() {
        float landDensityMax = .9f;
        float landDensityMin = .8f;
        float landDensityRange = landDensityMax - landDensityMin;
        float scaledLandDensity = mapParameters.getLandDensity() * landDensityRange + landDensityMin;
        int mapSize = map.getSize();

        land.setSize(mapSize / 16);

        land.randomize(scaledLandDensity).blur(2, .75f).erode(.5f);
        land.setSize(mapSize / 4);
        land.dilute(.5f, mapSize / 128);
        land.setSize(mapSize + 1);
        land.blur(8, .75f);

        if (mapSize <= 512) {
            land.add(connections.copy().inflate(mountainBrushSize / 8f).blur(12, .125f));
        }
    }

    protected void plateausSetup() {
        float plateauDensityMax = .7f;
        float plateauDensityMin = .6f;
        float plateauDensityRange = plateauDensityMax - plateauDensityMin;
        float scaledPlateauDensity = mapParameters.getPlateauDensity() * plateauDensityRange + plateauDensityMin;
        plateaus.setSize(map.getSize() / 16);

        plateaus.randomize(scaledPlateauDensity).blur(2, .75f).setSize(map.getSize() / 4);
        plateaus.dilute(.5f, map.getSize() / 128);
        plateaus.setSize(map.getSize() + 1);
        plateaus.blur(16, .75f);
    }

    protected void mountainSetup() {
        mountains.setSize(map.getSize() / 4);

        if (random.nextBoolean()) {
            mountains.progressiveWalk((int) (mapParameters.getMountainDensity() * 100 / mapParameters.getSymmetrySettings().getTerrainSymmetry().getNumSymPoints()), map.getSize() / 64);
        } else {
            mountains.randomWalk((int) (mapParameters.getMountainDensity() * 100 / mapParameters.getSymmetrySettings().getTerrainSymmetry().getNumSymPoints()), map.getSize() / 64);
        }
        mountains.dilute(.5f, 4);
        mountains.setSize(map.getSize() + 1);
    }

    protected void spawnTerrainSetup() {
        spawnPlateauMask.setSize(map.getSize() / 4);
        spawnPlateauMask.erode(.5f).dilute(.5f, 8);
        spawnPlateauMask.erode(.5f).setSize(map.getSize() + 1);
        spawnPlateauMask.blur(4);

        spawnLandMask.setSize(map.getSize() / 4);
        spawnLandMask.erode(.25f, map.getSize() / 128).dilute(.5f, 4);
        spawnLandMask.erode(.5f).setSize(map.getSize() + 1);
        spawnLandMask.blur(4);

        plateaus.subtract(spawnLandMask).add(spawnPlateauMask);
        land.add(spawnLandMask).add(spawnPlateauMask);
        if (map.getSize() > 512 && mapParameters.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints() <= 4) {
            land.add(spawnLandMask).add(spawnPlateauMask).inflate(16).deflate(16).setSize(map.getSize() / 8);
            land.erode(.5f, 10).add(spawnLandMask.copy().setSize(map.getSize() / 8)).add(spawnPlateauMask.copy().setSize(map.getSize() / 8))
                    .blur(4, .75f).dilute(.5f, 5).setSize(map.getSize() + 1);
            land.blur(8, .75f);
        } else {
            land.dilute(.25f, 16).blur(2);
        }

        ensureSpawnTerrain();

        mountains.multiply(mapParameters.getLandDensity() < .25f ? land.copy().deflate(24) : land);
    }

    protected void ensureSpawnTerrain() {
        mountains.subtract(connections.copy().inflate(mountainBrushSize / 4f).blur(16, .125f));
        mountains.subtract(spawnLandMask.copy().inflate(mountainBrushSize / 4f));

        plateaus.multiply(land).subtract(spawnLandMask).add(spawnPlateauMask);
        land.add(plateaus).add(spawnLandMask).add(spawnPlateauMask);
    }

    protected void enforceSymmetry() {
        SymmetrySettings symmetrySettings = heightmap.getSymmetrySettings();
        if (!symmetrySettings.getTerrainSymmetry().isPerfectSymmetry() && symmetrySettings.getSpawnSymmetry().isPerfectSymmetry()) {
            land.forceSymmetry();
            mountains.forceSymmetry();
            plateaus.forceSymmetry();
            ramps.forceSymmetry();
            hills.forceSymmetry();
            valleys.forceSymmetry();
            connections.forceSymmetry();
        }
    }

    protected void setupHeightmapPipeline() {
        int mapSize = map.getSize();
        int numBrushes = Brushes.GENERATOR_BRUSHES.size();

        String brush5 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(numBrushes));

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

        heightmapOcean.addDistance(land, -.45f).clampMin(oceanFloor).useBrushWithinAreaWithDensity(water.deflate(8).subtract(deepWater), brush5, shallowWaterBrushSize, shallowWaterBrushDensity, shallowWaterBrushIntensity, false)
                .useBrushWithinAreaWithDensity(deepWater, brush5, deepWaterBrushSize, deepWaterBrushDensity, deepWaterBrushIntensity, false).clampMax(0f).blur(4, deepWater).blur(1);

        heightmapLand.add(heightmapHills).add(heightmapValleys).add(heightmapMountains).add(landHeight)
                .add(heightmapPlateaus).setToValue(spawnLandMask, landHeight).setToValue(spawnPlateauMask, plateauHeight + landHeight)
                .blur(1, spawnLandMask.copy().inflate(4)).blur(1, spawnPlateauMask.copy().inflate(4)).add(heightmapOcean);

        heightmap.add(heightmapLand).add(waterHeight);

        if (heightMapNoise.getSymmetrySettings().getSpawnSymmetry().isPerfectSymmetry()) {
            heightMapNoise.addWhiteNoise(plateauHeight / 3).resample(mapSize / 64);
            heightMapNoise.addWhiteNoise(plateauHeight / 3).resample(mapSize + 1);
            heightMapNoise.addWhiteNoise(1)
                    .subtractAvg().clampMin(0f).setToValue(land.copy().invert().inflate(16), 0f)
                    .blur(mapSize / 16, spawnLandMask.copy().inflate(8))
                    .blur(mapSize / 16, spawnPlateauMask.copy().inflate(8)).blur(mapSize / 16);
            heightmap.add(heightMapNoise);
        }

        blurRamps();
    }

    private void blurRamps() {
        BooleanMask NoRamps = plateaus.copy().outline().subtract(ramps).add(mountains);
        BooleanMask inflatedRamps = ramps.copy();
        heightmap.blur(48, inflatedRamps)
                .blur(32, inflatedRamps.inflate(8).subtract(NoRamps.inflate(4)))
                .blur(4, inflatedRamps.copy().outline().inflate(4))
                .blur(24, inflatedRamps.inflate(8).subtract(NoRamps.inflate(4)))
                .blur(4, inflatedRamps.copy().outline().inflate(4))
                .blur(16, inflatedRamps.inflate(16).subtract(NoRamps.inflate(4)))
                .blur(4, inflatedRamps.copy().outline().inflate(4))
                .blur(8, inflatedRamps.inflate(16).subtract(NoRamps.inflate(4)))
                .blur(4, inflatedRamps.copy().outline().inflate(4))
                .clampMin(0f).clampMax(255f);
    }

    protected void initRamps() {
        float maxStepSize = map.getSize() / 128f;
        int maxMiddlePoints = 2;
        int numPaths = (int) (mapParameters.getRampDensity() * 20) / mapParameters.getSymmetrySettings().getTerrainSymmetry().getNumSymPoints();
        int bound = map.getSize() / 4;
        ramps.setSize(map.getSize() + 1);

        if (map.getSize() >= 512) {
            pathInEdgeBounds(ramps, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        } else {
            pathInEdgeBounds(ramps, maxStepSize, numPaths / 4, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        }

        ramps.subtract(connections.copy().inflate(64)).inflate(maxStepSize / 2f)
                .add(connections.copy().inflate(maxStepSize / 2f)).multiply(plateaus.copy().outline())
                .subtract(mountains).inflate(10);
    }

    protected void setupMountainHeightmapPipeline() {
        String brush = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));

        heightmapMountains.setSize(map.getSize() + 1);
        heightmapMountains.useBrushWithinAreaWithDensity(mountains, brush, mountainBrushSize, mountainBrushDensity, mountainBrushIntensity, false);

        BooleanMask paintedMountains = heightmapMountains.copyAsBooleanMask(plateauHeight / 2);

        mountains.init(paintedMountains);
        land.add(paintedMountains);

        heightmapMountains.blur(4, mountains.copy().inflate(64).subtract(mountains));
    }

    protected void setupPlateauHeightmapPipeline() {
        String brush = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));

        heightmapPlateaus.setSize(map.getSize() + 1);
        heightmapPlateaus.useBrushWithinAreaWithDensity(plateaus, brush, plateauBrushSize, plateauBrushDensity, plateauBrushIntensity, false).clampMax(plateauHeight);

        BooleanMask paintedPlateaus = heightmapPlateaus.copyAsBooleanMask(plateauHeight - 3);

        land.add(paintedPlateaus);
        plateaus.init(paintedPlateaus);
        plateaus.subtract(spawnLandMask).add(spawnPlateauMask);

        heightmapPlateaus.add(plateaus, 2f).clampMax(plateauHeight).blur(1, plateaus);

        BooleanMask plateauBase = heightmapPlateaus.copyAsBooleanMask(1f);

        heightmapPlateaus.blur(4, plateauBase.copy().inflate(96).subtract(plateauBase.copy().inflate(4)));
    }

    protected void setupSmallFeatureHeightmapPipeline() {
        int numSymPoints = mapParameters.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();
        String brushValley = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));
        String brushHill = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));

        heightmapValleys.setSize(map.getSize() + 1);
        heightmapHills.setSize(map.getSize() + 1);

        hills.setSize(map.getSize() / 4);
        valleys.setSize(map.getSize() / 4);

        hills.randomWalk(random.nextInt(4) + 1, random.nextInt(map.getSize() / 4) / numSymPoints).dilute(.5f, 2)
                .setSize(map.getSize() + 1);
        hills.multiply(land.copy().deflate(8)).subtract(plateaus.copy().outline().inflate(8)).subtract(spawnLandMask);
        valleys.randomWalk(random.nextInt(4), random.nextInt(map.getSize() / 4) / numSymPoints).dilute(.5f, 4)
                .setSize(map.getSize() + 1);
        valleys.multiply(plateaus.copy().deflate(8)).subtract(spawnPlateauMask);

        valleyBrushIntensity = -0.35f;
        heightmapValleys.useBrushWithinAreaWithDensity(valleys, brushValley, smallFeatureBrushSize, valleyBrushDensity, valleyBrushIntensity, false)
                .clampMin(valleyFloor);
        heightmapHills.useBrushWithinAreaWithDensity(hills.add(mountains.copy().outline().inflate(4).acid(.01f, 4)), brushHill, smallFeatureBrushSize, hillBrushDensity, hillBrushIntensity, false);
    }
}
