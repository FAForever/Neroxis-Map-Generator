package neroxis.generator.terrain;

import neroxis.brushes.Brushes;
import neroxis.map.*;
import neroxis.util.Vector3f;

public class BasicTerrainGenerator extends TerrainGenerator {
    protected ConcurrentBinaryMask spawnLandMask;
    protected ConcurrentBinaryMask spawnPlateauMask;
    protected ConcurrentBinaryMask land;
    protected ConcurrentBinaryMask mountains;
    protected ConcurrentBinaryMask hills;
    protected ConcurrentBinaryMask valleys;
    protected ConcurrentBinaryMask plateaus;
    protected ConcurrentBinaryMask ramps;
    protected ConcurrentBinaryMask connections;
    protected ConcurrentFloatMask heightmapValleys;
    protected ConcurrentFloatMask heightmapHills;
    protected ConcurrentFloatMask heightmapPlateaus;
    protected ConcurrentFloatMask heightmapMountains;
    protected ConcurrentFloatMask heightmapLand;
    protected ConcurrentFloatMask heightmapOcean;
    protected ConcurrentFloatMask noise;

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
        spawnLandMask = new ConcurrentBinaryMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "spawnLandMask");
        spawnPlateauMask = new ConcurrentBinaryMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "spawnPlateauMask");
        land = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "land");
        mountains = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "mountains");
        plateaus = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "plateaus");
        ramps = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "ramps");
        hills = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "hills");
        valleys = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "valleys");
        connections = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "connections");
        heightmapValleys = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "heightmapValleys");
        heightmapHills = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "heightmapHills");
        heightmapPlateaus = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "heightmapPlateaus");
        heightmapMountains = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "heightmapMountains");
        heightmapLand = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "heightmapLand");
        heightmapOcean = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "heightmapOcean");
        noise = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "noise");
        spawnSize = 48;
        waterHeight = mapParameters.getBiome().getWaterSettings().getElevation();
        plateauHeight = 5f;
        oceanFloor = -16f;
        valleyFloor = -5f;
        landHeight = .05f;

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
        spawnTerrainSetup();
        if (mapParameters.getSymmetrySettings().getSpawnSymmetry().isPerfectSymmetry()) {
            setupHeightmapPipeline();
        } else {
            setupSimpleHeightmapPipeline();
        }
    }

    protected void spawnMaskSetup() {
        map.getSpawns().forEach(spawn -> {
            Vector3f location = spawn.getPosition();
            spawnLandMask.fillCircle(location, spawnSize, true);
        });

        if (random.nextFloat() < mapParameters.getPlateauDensity()) {
            map.getSpawns().forEach(spawn -> {
                Vector3f location = spawn.getPosition();
                spawnPlateauMask.fillCircle(location, spawnSize, true);
            });
        }
    }

    protected void teamConnectionsSetup() {
        float maxStepSize = map.getSize() / 128f;
        int minMiddlePoints = 0;
        int maxMiddlePoints = 2;
        int numTeamConnections = (int) ((mapParameters.getRampDensity() + mapParameters.getPlateauDensity() + (1 - mapParameters.getMountainDensity())) / 3 * 2 + 1);
        int numTeammateConnections = 1;
        connections.setSize(map.getSize() + 1);

        connectTeamsAroundCenter(connections, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize);
        connectTeammates(connections, maxMiddlePoints, numTeammateConnections, maxStepSize);
    }

    protected void landSetup() {
        float landDensityMax = .9f;
        float landDensityMin = .8f;
        float landDensityRange = landDensityMax - landDensityMin;
        float scaledLandDensity = mapParameters.getLandDensity() * landDensityRange + landDensityMin;
        int mapSize = map.getSize();
        land.setSize(mapSize / 16);

        land.randomize(scaledLandDensity).smooth(2, .75f).erode(.5f, SymmetryType.TERRAIN, mapSize / 256);
        land.setSize(mapSize / 4);
        land.grow(.5f, SymmetryType.TERRAIN, mapSize / 128);
        land.setSize(mapSize + 1);
        land.smooth(8, .75f);

        if (mapSize <= 512) {
            land.combine(connections.copy().inflate(mountainBrushSize / 8f).smooth(12, .125f));
        }
    }

    protected void plateausSetup() {
        float plateauDensityMax = .7f;
        float plateauDensityMin = .6f;
        float plateauDensityRange = plateauDensityMax - plateauDensityMin;
        float scaledPlateauDensity = mapParameters.getPlateauDensity() * plateauDensityRange + plateauDensityMin;
        plateaus.setSize(map.getSize() / 16);

        plateaus.randomize(scaledPlateauDensity).smooth(2, .75f).setSize(map.getSize() / 4);
        plateaus.grow(.5f, SymmetryType.TERRAIN, map.getSize() / 128);
        plateaus.setSize(map.getSize() + 1);
        plateaus.smooth(16, .25f);
    }

    protected void mountainSetup() {
        mountains.setSize(map.getSize() / 4);

        if (random.nextBoolean()) {
            mountains.progressiveWalk((int) (mapParameters.getMountainDensity() * 100 / mapParameters.getSymmetrySettings().getTerrainSymmetry().getNumSymPoints()), map.getSize() / 16);
        } else {
            mountains.randomWalk((int) (mapParameters.getMountainDensity() * 100 / mapParameters.getSymmetrySettings().getTerrainSymmetry().getNumSymPoints()), map.getSize() / 16);
        }
        mountains.grow(.5f, SymmetryType.TERRAIN, 2);
        mountains.setSize(map.getSize() + 1);
    }

    protected void spawnTerrainSetup() {
        spawnPlateauMask.setSize(map.getSize() / 4);
        spawnPlateauMask.erode(.5f, SymmetryType.SPAWN, 4).grow(.5f, SymmetryType.SPAWN, 8);
        spawnPlateauMask.erode(.5f, SymmetryType.SPAWN).setSize(map.getSize() + 1);
        spawnPlateauMask.smooth(4);

        spawnLandMask.setSize(map.getSize() / 4);
        spawnLandMask.erode(.25f, SymmetryType.SPAWN, map.getSize() / 128).grow(.5f, SymmetryType.SPAWN, 4);
        spawnLandMask.erode(.5f, SymmetryType.SPAWN).setSize(map.getSize() + 1);
        spawnLandMask.smooth(4);

        plateaus.minus(spawnLandMask).combine(spawnPlateauMask);
        land.combine(spawnLandMask).combine(spawnPlateauMask);
        if (map.getSize() > 512 && mapParameters.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints() <= 4) {
            land.combine(spawnLandMask).combine(spawnPlateauMask).inflate(16).deflate(16).setSize(map.getSize() / 8);
            land.erode(.5f, SymmetryType.SPAWN, 10).combine((ConcurrentBinaryMask) spawnLandMask.copy().setSize(map.getSize() / 8)).combine((ConcurrentBinaryMask) spawnPlateauMask.copy().setSize(map.getSize() / 8))
                    .smooth(4, .75f).grow(.5f, SymmetryType.SPAWN, 5).setSize(map.getSize() + 1);
            land.smooth(8, .75f);
        } else {
            land.grow(.25f, SymmetryType.SPAWN, 16).smooth(2);
        }

        ensureSpawnTerrain();

        mountains.intersect(mapParameters.getLandDensity() < .25f ? land.copy().deflate(24) : land);
    }

    protected void ensureSpawnTerrain() {
        mountains.minus(connections.copy().inflate(mountainBrushSize / 2f).smooth(12, .125f));
        mountains.minus(spawnLandMask.copy().inflate(mountainBrushSize / 2f));

        plateaus.intersect(land).minus(spawnLandMask).combine(spawnPlateauMask);
        land.combine(plateaus).combine(spawnLandMask).combine(spawnPlateauMask);
    }

    protected void setupHeightmapPipeline() {
        int mapSize = map.getSize();
        int numBrushes = Brushes.GENERATOR_BRUSHES.size();

        String brush5 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(numBrushes));

        setupMountainHeightmapPipeline();
        setupPlateauHeightmapPipeline();
        setupSmallFeatureHeightmapPipeline();
        initRamps();

        ConcurrentBinaryMask water = land.copy().invert();
        ConcurrentBinaryMask deepWater = water.copy().deflate(32);

        heightmap.setSize(mapSize + 1);
        heightmapLand.setSize(mapSize + 1);
        heightmapOcean.setSize(mapSize + 1);
        noise.setSize(mapSize / 128);

        heightmapOcean.addDistance(land, -.45f).clampMin(oceanFloor).useBrushWithinAreaWithDensity(water.deflate(8).minus(deepWater), brush5, shallowWaterBrushSize, shallowWaterBrushDensity, shallowWaterBrushIntensity, false)
                .useBrushWithinAreaWithDensity(deepWater, brush5, deepWaterBrushSize, deepWaterBrushDensity, deepWaterBrushIntensity, false).clampMax(0).smooth(4, deepWater).smooth(1);

        heightmapLand.add(heightmapHills).add(heightmapValleys).add(heightmapMountains).add(landHeight)
                .setToValue(landHeight, spawnLandMask).add(heightmapPlateaus).setToValue(plateauHeight + landHeight, spawnPlateauMask)
                .smooth(1, spawnPlateauMask.copy().inflate(4)).add(heightmapOcean);

        noise.addWhiteNoise(plateauHeight / 2).resample(mapSize / 64).addWhiteNoise(plateauHeight / 2).resample(mapSize + 1).addWhiteNoise(1)
                .subtractAvg().clampMin(0f).setToValue(0f, land.copy().invert().inflate(16)).smooth(mapSize / 16, spawnLandMask.copy().inflate(8))
                .smooth(mapSize / 16, spawnPlateauMask.copy().inflate(8)).smooth(mapSize / 16);

        heightmap.add(heightmapLand).add(waterHeight).add(noise).smooth(8, ramps.copy().acid(.001f, 4).erode(.25f, SymmetryType.SPAWN, 4))
                .smooth(6, ramps.copy().inflate(8).acid(.01f, 4).erode(.25f, SymmetryType.SPAWN, 4))
                .smooth(4, ramps.copy().inflate(12)).smooth(4, ramps.copy().inflate(16)).clampMin(0f).clampMax(255f);
    }

    protected void setupSimpleHeightmapPipeline() {
        int mapSize = map.getSize();
        float waterHeight = mapParameters.getBiome().getWaterSettings().getElevation();

        ConcurrentBinaryMask symmetryLimits = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), mapParameters.getSymmetrySettings(), "symmetryLimits");
        symmetryLimits.fillCircle((mapSize + 1) / 2f, (mapSize + 1) / 2f, (mapSize - 32) / 2f, true).setSize(mapSize / 8);
        symmetryLimits.inflate(1).erode(.5f, SymmetryType.SPAWN, 6).inflate(2);
        symmetryLimits.setSize(mapSize + 1);
        symmetryLimits.inflate(4);

        heightmap.setSize(mapSize + 1);
        heightmapMountains.setSize(mapSize + 1);
        heightmapPlateaus.setSize(mapSize + 1);
        heightmapLand.setSize(mapSize + 1);
        heightmapOcean.setSize(mapSize + 1);
        noise.setSize(mapSize / 128);

        land.intersect(symmetryLimits);
        plateaus.intersect(symmetryLimits);
        mountains.intersect(symmetryLimits);

        heightmapPlateaus.addDistance(plateaus.grow(1, SymmetryType.SPAWN).inflate(1).invert(), 1).clampMax(plateauHeight);
        heightmapMountains.addDistance(mountains.grow(1, SymmetryType.SPAWN).inflate(1).invert(), 1f);
        heightmapOcean.addDistance(land, -.45f).clampMin(oceanFloor);

        ConcurrentBinaryMask paintedPlateaus = new ConcurrentBinaryMask(heightmapPlateaus, plateauHeight - 3, random.nextLong(), "paintedPlateaus");
        ConcurrentBinaryMask paintedMountains = new ConcurrentBinaryMask(heightmapMountains, plateauHeight / 2, random.nextLong(), "paintedMountains");

        land.combine(paintedPlateaus);
        plateaus.replace(paintedPlateaus);
        plateaus.minus(spawnLandMask).combine(spawnPlateauMask);
        mountains.replace(paintedMountains);
        land.combine(paintedMountains);

        heightmapPlateaus.add(plateaus, 3).clampMax(plateauHeight).smooth(1, plateaus);
        plateaus.minus(spawnLandMask).combine(spawnPlateauMask);

        initRamps();

        heightmapLand.add(heightmapMountains).add(landHeight)
                .setToValue(landHeight, spawnLandMask).add(heightmapPlateaus).setToValue(plateauHeight + landHeight, spawnPlateauMask)
                .smooth(1, spawnPlateauMask.copy().inflate(4)).add(heightmapOcean);

        heightmap.add(heightmapLand).add(waterHeight).smooth(8, ramps.copy().acid(.001f, 4).erode(.25f, SymmetryType.SPAWN, 4))
                .smooth(6, ramps.copy().inflate(8).acid(.01f, 4).erode(.25f, SymmetryType.SPAWN, 4))
                .smooth(4, ramps.copy().inflate(12)).smooth(4, ramps.copy().inflate(16)).clampMin(0f).clampMax(255f);
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

        ramps.minus(connections.copy().inflate(32)).inflate(maxStepSize / 2f).intersect(plateaus.copy().outline())
                .space(6, 12).combine(connections.copy().inflate(maxStepSize / 2f).intersect(plateaus.copy().outline()))
                .minus(mountains).inflate(24);
    }

    protected void setupMountainHeightmapPipeline() {
        String brush = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));

        heightmapMountains.setSize(map.getSize() + 1);
        heightmapMountains.useBrushWithinAreaWithDensity(mountains, brush, mountainBrushSize, mountainBrushDensity, mountainBrushIntensity, false);

        ConcurrentBinaryMask paintedMountains = new ConcurrentBinaryMask(heightmapMountains, plateauHeight / 2, random.nextLong(), "paintedMountains");

        mountains.replace(paintedMountains);
        land.combine(paintedMountains);

        heightmapMountains.smooth(4, mountains.copy().inflate(32).minus(mountains.copy().inflate(4)));
    }

    protected void setupPlateauHeightmapPipeline() {
        String brush = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));

        heightmapPlateaus.setSize(map.getSize() + 1);
        heightmapPlateaus.useBrushWithinAreaWithDensity(plateaus, brush, plateauBrushSize, plateauBrushDensity, plateauBrushIntensity, false).clampMax(plateauHeight);

        ConcurrentBinaryMask paintedPlateaus = new ConcurrentBinaryMask(heightmapPlateaus, plateauHeight - 3, random.nextLong(), "paintedPlateaus");

        land.combine(paintedPlateaus);
        plateaus.replace(paintedPlateaus);

        heightmapPlateaus.add(plateaus, 3).clampMax(plateauHeight).smooth(1, plateaus);
        plateaus.minus(spawnLandMask).combine(spawnPlateauMask);

        ConcurrentBinaryMask plateauBase = new ConcurrentBinaryMask(heightmapPlateaus, 1, random.nextLong(), "plateauBase");

        plateauBase.outline().inflate(4);
        heightmapPlateaus.smooth(1, plateauBase);
    }

    protected void setupSmallFeatureHeightmapPipeline() {
        int numSymPoints = mapParameters.getSymmetrySettings().getSpawnSymmetry().getNumSymPoints();
        String brushValley = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));
        String brushHill = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));

        heightmapValleys.setSize(map.getSize() + 1);
        heightmapHills.setSize(map.getSize() + 1);

        hills.setSize(map.getSize() / 4);
        valleys.setSize(map.getSize() / 4);

        hills.randomWalk(random.nextInt(4) + 1, random.nextInt(map.getSize() / 2) / numSymPoints).grow(.5f, SymmetryType.SPAWN, 2)
                .setSize(map.getSize() + 1);
        hills.intersect(land.copy().deflate(8)).minus(plateaus.copy().outline().inflate(8)).minus(spawnLandMask);
        valleys.randomWalk(random.nextInt(4), random.nextInt(map.getSize() / 2) / numSymPoints).grow(.5f, SymmetryType.SPAWN, 4)
                .setSize(map.getSize() + 1);
        valleys.intersect(plateaus.copy().deflate(8)).minus(spawnPlateauMask);

        valleyBrushIntensity = -0.35f;
        heightmapValleys.useBrushWithinAreaWithDensity(valleys, brushValley, smallFeatureBrushSize, valleyBrushDensity, valleyBrushIntensity, false)
                .clampMin(valleyFloor);
        heightmapHills.useBrushWithinAreaWithDensity(hills.combine(mountains.copy().outline().inflate(4).acid(.01f, 4)), brushHill, smallFeatureBrushSize, hillBrushDensity, hillBrushIntensity, false);
    }
}
