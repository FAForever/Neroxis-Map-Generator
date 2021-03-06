package neroxis.generator.mapstyles;

import lombok.Getter;
import lombok.Setter;
import neroxis.biomes.Biome;
import neroxis.brushes.Brushes;
import neroxis.generator.*;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;
import neroxis.util.Vector2f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Getter
@Setter
public strictfp class DefaultStyleGenerator {

    public static boolean DEBUG = MapGenerator.DEBUG;

    protected final MapParameters mapParameters;
    protected final SCMap map;
    protected final int spawnCount;
    protected final float landDensity;
    protected final float plateauDensity;
    protected final float mountainDensity;
    protected final float rampDensity;
    protected final int mapSize;
    protected final int numTeams;
    protected final float reclaimDensity;
    protected final int mexCount;
    protected final int hydroCount;
    protected final SymmetrySettings symmetrySettings;
    protected final Biome biome;
    protected final boolean unexplored;

    //masks used in generation
    protected final ConcurrentBinaryMask land;
    protected final ConcurrentBinaryMask mountains;
    protected final ConcurrentBinaryMask hills;
    protected final ConcurrentBinaryMask valleys;
    protected final ConcurrentBinaryMask plateaus;
    protected final ConcurrentBinaryMask ramps;
    protected final ConcurrentBinaryMask connections;
    protected final ConcurrentBinaryMask impassable;
    protected final ConcurrentBinaryMask unbuildable;
    protected final ConcurrentBinaryMask notFlat;
    protected final ConcurrentBinaryMask passable;
    protected final ConcurrentBinaryMask passableLand;
    protected final ConcurrentBinaryMask passableWater;
    protected final ConcurrentBinaryMask t1LandWreckMask;
    protected final ConcurrentBinaryMask t2LandWreckMask;
    protected final ConcurrentBinaryMask t3LandWreckMask;
    protected final ConcurrentBinaryMask t2NavyWreckMask;
    protected final ConcurrentBinaryMask navyFactoryWreckMask;
    protected final ConcurrentBinaryMask allWreckMask;
    protected final ConcurrentBinaryMask treeMask;
    protected final ConcurrentBinaryMask cliffRockMask;
    protected final ConcurrentBinaryMask fieldStoneMask;
    protected final ConcurrentBinaryMask largeRockFieldMask;
    protected final ConcurrentBinaryMask smallRockFieldMask;
    protected final ConcurrentBinaryMask baseMask;
    protected final ConcurrentBinaryMask civReclaimMask;
    protected final ConcurrentBinaryMask allBaseMask;
    protected final ConcurrentFloatMask slope;
    protected final ConcurrentFloatMask heightmapBase;
    protected final ConcurrentBinaryMask spawnLandMask;
    protected final ConcurrentBinaryMask spawnPlateauMask;
    protected final ConcurrentBinaryMask fieldDecal;
    protected final ConcurrentBinaryMask slopeDecal;
    protected final ConcurrentBinaryMask mountainDecal;
    protected final ConcurrentBinaryMask resourceMask;
    protected final ConcurrentBinaryMask waterResourceMask;
    protected final ConcurrentFloatMask accentGroundTexture;
    protected final ConcurrentFloatMask waterBeachTexture;
    protected final ConcurrentFloatMask accentSlopesTexture;
    protected final ConcurrentFloatMask accentPlateauTexture;
    protected final ConcurrentFloatMask slopesTexture;
    protected final ConcurrentFloatMask steepHillsTexture;
    protected final ConcurrentFloatMask rockTexture;
    protected final ConcurrentFloatMask accentRockTexture;
    protected final BinaryMask noProps;
    protected final BinaryMask noWrecks;
    protected final BinaryMask noBases;
    protected final BinaryMask noCivs;
    protected final SpawnGenerator spawnGenerator;
    protected final MexGenerator mexGenerator;
    protected final HydroGenerator hydroGenerator;
    protected final PropGenerator propGenerator;
    protected final DecalGenerator decalGenerator;
    protected final UnitGenerator unitGenerator;
    protected Random random;
    protected float shallowWaterBrushIntensity;
    protected float deepWaterBrushIntensity;
    protected float plateauBrushDensity;
    protected float valleyBrushDensity;
    protected float hillBrushDensity;
    protected int shallowWaterBrushSize;
    protected float shallowWaterBrushDensity;
    protected int deepWaterBrushSize;
    protected float deepWaterBrushDensity;
    protected boolean generationComplete;
    protected float waterHeight;
    protected boolean hasCivilians;
    protected boolean enemyCivilians;
    protected float plateauHeight;
    protected float oceanFloor;
    protected float valleyFloor;
    protected float landHeight;
    protected int mountainBrushSize;
    protected int plateauBrushSize;
    protected int smallFeatureBrushSize;
    protected int spawnSize;
    protected float mountainBrushIntensity;
    protected float plateauBrushIntensity;
    protected float valleyBrushIntensity;
    protected float hillBrushIntensity;
    protected float mountainBrushDensity;

    public DefaultStyleGenerator(MapParameters mapParameters, Random random) {
        this.mapParameters = mapParameters;
        this.random = random;
        landDensity = mapParameters.getLandDensity();
        mountainDensity = mapParameters.getMountainDensity();
        plateauDensity = mapParameters.getPlateauDensity();
        rampDensity = mapParameters.getRampDensity();
        reclaimDensity = mapParameters.getReclaimDensity();
        mexCount = mapParameters.getMexCount();
        hydroCount = mapParameters.getHydroCount();
        spawnCount = mapParameters.getSpawnCount();
        mapSize = mapParameters.getMapSize();
        numTeams = mapParameters.getNumTeams();
        symmetrySettings = mapParameters.getSymmetrySettings();
        unexplored = mapParameters.isUnexplored();
        biome = mapParameters.getBiome();
        waterHeight = biome.getWaterSettings().getElevation();
        plateauHeight = 6f;
        oceanFloor = -16f;
        valleyFloor = -5f;
        landHeight = .05f;
        map = new SCMap(mapSize, spawnCount, mexCount * spawnCount, hydroCount, biome);

        spawnGenerator = new SpawnGenerator(map, random.nextLong());
        mexGenerator = new MexGenerator(map, random.nextLong());
        hydroGenerator = new HydroGenerator(map, random.nextLong());
        propGenerator = new PropGenerator(map, random.nextLong());
        decalGenerator = new DecalGenerator(map, random.nextLong());
        unitGenerator = new UnitGenerator(random.nextLong());

        Pipeline.reset();

        land = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "land");
        mountains = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "mountains");
        plateaus = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "plateaus");
        ramps = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "ramps");
        hills = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "hills");
        valleys = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "valleys");
        connections = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "connections");
        impassable = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "impassable");
        unbuildable = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "unbuildable");
        notFlat = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "notFlat");
        passable = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "passable");
        passableLand = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "passableLand");
        passableWater = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "passableWater");
        spawnLandMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "land");
        spawnPlateauMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "spawnPlateauMask");
        resourceMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "resourceMask");
        waterResourceMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "waterResourceMask");
        fieldDecal = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "fieldDecal");
        slopeDecal = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "slopeDecal");
        mountainDecal = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "mountainDecal");
        t1LandWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "t1LandWreckMask");
        t2LandWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "t2LandWreckMask");
        t3LandWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "t3LandWreckMask");
        t2NavyWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "t2NavyWreckMask");
        navyFactoryWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "navyFactoryWreckMask");
        allWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "allWreckMask");
        treeMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "treeMask");
        cliffRockMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "cliffRockMask");
        fieldStoneMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "fieldStoneMask");
        largeRockFieldMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "largeRockFieldMask");
        smallRockFieldMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "smallRockFieldMask");
        baseMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "baseMask");
        civReclaimMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "civReclaimMask");
        allBaseMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "allBaseMask");
        accentGroundTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentGroundTexture");
        waterBeachTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "waterBeachTexture");
        accentSlopesTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentSlopesTexture");
        accentPlateauTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentPlateauTexture");
        slopesTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "slopesTexture");
        steepHillsTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "steepHillsTexture");
        rockTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "rockTexture");
        accentRockTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentRockTexture");
        slope = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "slope");
        heightmapBase = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "heightmapBase");
        noProps = new BinaryMask(1, random.nextLong(), symmetrySettings);
        noWrecks = new BinaryMask(1, random.nextLong(), symmetrySettings);
        noBases = new BinaryMask(1, random.nextLong(), symmetrySettings);
        noCivs = new BinaryMask(1, random.nextLong(), symmetrySettings);
        mountainBrushSize = 64;
        plateauBrushSize = 32;
        smallFeatureBrushSize = 24;
        spawnSize = 36;
        if (mapSize < 512) {
            mountainBrushSize = 32;
        }
        hillBrushIntensity = 0.5f;
        mountainBrushIntensity = 10f;
        plateauBrushIntensity = 8f;
        mountainBrushDensity = mapSize < 512 ? .1f : .05f;
        shallowWaterBrushIntensity = .5f;
        deepWaterBrushIntensity = 1f;
        plateauBrushDensity = .64f;
        valleyBrushDensity = .72f;
        hillBrushDensity = .72f;
        shallowWaterBrushSize = 24;
        shallowWaterBrushDensity = 1f;
        deepWaterBrushSize = 64;
        deepWaterBrushDensity = .065f;
    }

    public SCMap generate() {
        hasCivilians = random.nextBoolean() && !unexplored;
        enemyCivilians = random.nextBoolean() && hasCivilians;

        int spawnSeparation = random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16;

        BinaryMask[] spawnMasks = spawnGenerator.generateSpawns(spawnSeparation, symmetrySettings, plateauDensity, spawnSize);
        spawnLandMask.init(spawnMasks[0]);
        spawnPlateauMask.init(spawnMasks[1]);

        setupPipeline();

        random = null;
        Pipeline.start();

        CompletableFuture<Void> aiMarkerFuture = CompletableFuture.runAsync(this::generateAIMarkers);
        CompletableFuture<Void> textureFuture = CompletableFuture.runAsync(this::addTextures);
        CompletableFuture<Void> resourcesFuture = CompletableFuture.runAsync(this::generateResources);
        CompletableFuture<Void> decalsFuture = CompletableFuture.runAsync(this::generateDecals);

        resourcesFuture.join();

        CompletableFuture<Void> propsFuture = CompletableFuture.runAsync(this::generateProps);
        CompletableFuture<Void> unitsFuture = CompletableFuture.runAsync(this::generateUnits);
        CompletableFuture<Void> heightMapFuture = CompletableFuture.runAsync(this::setHeightmap);

        propsFuture.join();
        decalsFuture.join();
        aiMarkerFuture.join();
        heightMapFuture.join();
        unitsFuture.join();

        CompletableFuture<Void> placementFuture = CompletableFuture.runAsync(this::setHeights);

        textureFuture.join();
        placementFuture.join();
        Pipeline.stop();
        return map;
    }

    protected void setupPipeline() {
        setupTerrainPipeline();
        setupHeightmapPipeline();
        setupTexturePipeline();
        setupPropPipeline();
        setupWreckPipeline();
        setupResourcePipeline();
        setupDecalPipeline();
    }

    protected void setupTerrainPipeline() {
        teamConnectionsInit();
        landInit();
        plateausInit();
        mountainInit();

        addSpawnTerrain();
    }

    protected void teamConnectionsInit() {
        float maxStepSize = mapSize / 128f;
        int minMiddlePoints = 0;
        int maxMiddlePoints = 1;
        int numTeamConnections = (int) ((rampDensity + plateauDensity + (1 - mountainDensity)) / 3 * 2 + 1 + spawnCount / 4);
        int numTeammateConnections = 1;
        connections.setSize(mapSize + 1);

        connectTeams(connections, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize);
        connectTeammates(connections, maxMiddlePoints, numTeammateConnections, maxStepSize);
    }

    protected void landInit() {
        float landDensityMax = .9f;
        float landDensityMin = .8f;
        float landDensityRange = landDensityMax - landDensityMin;
        float scaledLandDensity = landDensity * landDensityRange + landDensityMin;
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

    protected void plateausInit() {
        float plateauDensityMax = .7f;
        float plateauDensityMin = .6f;
        float plateauDensityRange = plateauDensityMax - plateauDensityMin;
        float scaledPlateauDensity = plateauDensity * plateauDensityRange + plateauDensityMin;
        plateaus.setSize(mapSize / 16);

        plateaus.randomize(scaledPlateauDensity).smooth(2, .75f).setSize(mapSize / 4);
        plateaus.grow(.5f, SymmetryType.TERRAIN, mapSize / 128);
        plateaus.setSize(mapSize + 1);
        plateaus.smooth(8, .75f);
    }

    protected void mountainInit() {
        mountains.setSize(mapSize / 4);

        if (random.nextBoolean()) {
            mountains.progressiveWalk((int) (mountainDensity * 100 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()), mapSize / 16);
        } else {
            mountains.randomWalk((int) (mountainDensity * 100 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()), mapSize / 16);
        }
        mountains.grow(.5f, SymmetryType.TERRAIN, 2);
        mountains.setSize(mapSize + 1);
    }

    protected void addSpawnTerrain() {
        spawnPlateauMask.setSize(mapSize / 4);
        spawnPlateauMask.erode(.5f, SymmetryType.SPAWN, 4).grow(.5f, SymmetryType.SPAWN, 8);
        spawnPlateauMask.erode(.5f, SymmetryType.SPAWN).setSize(mapSize + 1);
        spawnPlateauMask.smooth(4);

        spawnLandMask.setSize(mapSize / 4);
        spawnLandMask.erode(.25f, SymmetryType.SPAWN, mapSize / 128).grow(.5f, SymmetryType.SPAWN, 4);
        spawnLandMask.erode(.5f, SymmetryType.SPAWN).setSize(mapSize + 1);
        spawnLandMask.smooth(4);

        plateaus.minus(spawnLandMask).combine(spawnPlateauMask);
        land.combine(spawnLandMask).combine(spawnPlateauMask);
        if (mapSize > 512) {
            land.combine(spawnLandMask).combine(spawnPlateauMask).inflate(16).deflate(16).setSize(mapSize / 8);
            land.erode(.5f, SymmetryType.SPAWN, 10).combine((ConcurrentBinaryMask) spawnLandMask.copy().setSize(mapSize / 8)).combine((ConcurrentBinaryMask) spawnPlateauMask.copy().setSize(mapSize / 8))
                    .smooth(4, .75f).grow(.5f, SymmetryType.SPAWN, 5).setSize(mapSize + 1);
            land.smooth(8, .75f);
        } else {
            land.grow(.25f, SymmetryType.SPAWN, 16).smooth(2);
        }

        mountains.minus(connections.copy().inflate(mountainBrushSize / 2f).smooth(12, .125f));
        mountains.minus(spawnLandMask.copy().inflate(mountainBrushSize / 2f));

        plateaus.intersect(land).minus(spawnLandMask).combine(spawnPlateauMask);
        land.combine(plateaus).combine(spawnLandMask).combine(spawnPlateauMask);

        mountains.intersect(landDensity < .25f ? land.copy().deflate(24) : land);
    }

    protected void initRamps() {
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 2;
        int numPaths = (int) (rampDensity * 20) / symmetrySettings.getTerrainSymmetry().getNumSymPoints();
        int bound = mapSize / 4;
        ramps.setSize(mapSize + 1);

        if (mapSize >= 512) {
            pathInEdgeBounds(ramps, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        } else {
            pathInEdgeBounds(ramps, maxStepSize, numPaths / 4, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        }

        ramps.minus(connections.copy().inflate(32)).inflate(maxStepSize / 2f).intersect(plateaus.copy().outline())
                .space(6, 12).combine(connections.copy().inflate(maxStepSize / 2f).intersect(plateaus.copy().outline()))
                .inflate(24);
    }

    protected void setupHeightmapPipeline() {
        int numSymPoints = symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int numBrushes = Brushes.GENERATOR_BRUSHES.size();
        float waterHeight = biome.getWaterSettings().getElevation();
        String brush1 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(numBrushes));
        String brush2 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(numBrushes));
        String brush3 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(numBrushes));
        String brush4 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(numBrushes));
        String brush5 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(numBrushes));

        heightmapBase.setSize(mapSize + 1);
        ConcurrentFloatMask heightmapValleys = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapValleys");
        ConcurrentFloatMask heightmapHills = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapHills");
        ConcurrentFloatMask heightmapPlateaus = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapPlateaus");
        ConcurrentFloatMask heightmapMountains = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapMountains");
        ConcurrentFloatMask heightmapLand = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapLand");
        ConcurrentFloatMask heightmapOcean = new ConcurrentFloatMask(mapSize + 1, random.nextLong(), symmetrySettings, "heightmapOcean");
        ConcurrentFloatMask noise = new ConcurrentFloatMask(mapSize / 128, random.nextLong(), symmetrySettings, "noise");

        heightmapMountains.useBrushWithinAreaWithDensity(mountains, brush3, mountainBrushSize, mountainBrushDensity, mountainBrushIntensity, false);

        ConcurrentBinaryMask paintedMountains = new ConcurrentBinaryMask(heightmapMountains, plateauHeight / 2, random.nextLong(), "paintedMountains");

        mountains.replace(paintedMountains);
        land.combine(paintedMountains);

        heightmapMountains.smooth(4, mountains.copy().inflate(32).minus(mountains.copy().inflate(4)));

        heightmapPlateaus.useBrushWithinAreaWithDensity(plateaus, brush1, plateauBrushSize, plateauBrushDensity, plateauBrushIntensity, false).clampMax(plateauHeight);

        ConcurrentBinaryMask paintedPlateaus = new ConcurrentBinaryMask(heightmapPlateaus, plateauHeight - 2f, random.nextLong(), "paintedPlateaus");

        land.combine(paintedPlateaus);
        plateaus.replace(paintedPlateaus);

        heightmapPlateaus.add(plateaus, 2f).clampMax(plateauHeight).smooth(1, plateaus);

        plateaus.minus(spawnLandMask).combine(spawnPlateauMask);

        hills.setSize(mapSize / 4);
        valleys.setSize(mapSize / 4);

        hills.randomWalk(random.nextInt(4) + 1, random.nextInt(mapSize / 2) / numSymPoints).grow(.5f, SymmetryType.SPAWN, 2)
                .setSize(mapSize + 1);
        hills.intersect(land.copy().deflate(8)).minus(plateaus.copy().outline().inflate(8)).minus(spawnLandMask);
        valleys.randomWalk(random.nextInt(4), random.nextInt(mapSize / 2) / numSymPoints).grow(.5f, SymmetryType.SPAWN, 4)
                .setSize(mapSize + 1);
        valleys.intersect(plateaus.copy().deflate(8)).minus(spawnPlateauMask);

        valleyBrushIntensity = -0.35f;
        heightmapValleys.useBrushWithinAreaWithDensity(valleys, brush2, smallFeatureBrushSize, valleyBrushDensity, valleyBrushIntensity, false)
                .clampMin(valleyFloor);
        heightmapHills.useBrushWithinAreaWithDensity(hills.combine(mountains.copy().outline().inflate(4).acid(.01f, 4)), brush4, smallFeatureBrushSize, hillBrushDensity, hillBrushIntensity, false);

        initRamps();

        ConcurrentBinaryMask water = land.copy().invert();
        ConcurrentBinaryMask deepWater = water.copy().deflate(32);

        heightmapOcean.addDistance(land, -.45f).clampMin(oceanFloor).useBrushWithinAreaWithDensity(water.deflate(8).minus(deepWater), brush5, shallowWaterBrushSize, shallowWaterBrushDensity, shallowWaterBrushIntensity, false)
                .useBrushWithinAreaWithDensity(deepWater, brush5, deepWaterBrushSize, deepWaterBrushDensity, deepWaterBrushIntensity, false).clampMax(0).smooth(4, deepWater).smooth(1);

        heightmapLand.add(heightmapHills).add(heightmapValleys).add(heightmapMountains).add(landHeight)
                .setToValue(landHeight, spawnLandMask).add(heightmapPlateaus).setToValue(plateauHeight + landHeight, spawnPlateauMask)
                .smooth(1, spawnPlateauMask.copy().inflate(4)).add(heightmapOcean);

        heightmapBase.add(heightmapLand);

        noise.addWhiteNoise(plateauHeight / 2).resample(mapSize / 64).addWhiteNoise(plateauHeight / 2).resample(mapSize + 1).addWhiteNoise(1)
                .subtractAvg().clampMin(0f).setToValue(0f, land.copy().invert().inflate(16)).smooth(mapSize / 16, spawnLandMask.copy().inflate(8))
                .smooth(mapSize / 16, spawnPlateauMask.copy().inflate(8)).smooth(mapSize / 16);

        heightmapBase.add(waterHeight).add(noise).smooth(8, ramps.copy().acid(.001f, 4).erode(.25f, SymmetryType.SPAWN, 4))
                .smooth(6, ramps.copy().inflate(8).acid(.01f, 4).erode(.25f, SymmetryType.SPAWN, 4))
                .smooth(4, ramps.copy().inflate(12)).smooth(4, ramps.copy().inflate(16)).clampMin(0f).clampMax(255f);

        ConcurrentBinaryMask paintedLand = new ConcurrentBinaryMask(heightmapBase, waterHeight, random.nextLong(), "paintedLand");

        land.replace(paintedLand);

        slope.init(heightmapBase.copy().supcomGradient());

        impassable.init(slope, .7f);
        unbuildable.init(slope, .1f);
        notFlat.init(slope, .05f);

        impassable.inflate(2).combine(paintedMountains);

        passable.init(impassable).invert();
        passableLand.init(land);
        passableWater.init(land).invert();

        passable.fillEdge(8, false);
        passableLand.intersect(passable);
        passableWater.deflate(16).fillEdge(8, false);
    }

    protected void setupPropPipeline() {
        baseMask.setSize(mapSize / 4);
        civReclaimMask.setSize(mapSize / 4);
        allBaseMask.setSize(mapSize + 1);
        treeMask.setSize(mapSize / 16);
        cliffRockMask.setSize(mapSize / 16);
        fieldStoneMask.setSize(mapSize / 4);
        largeRockFieldMask.setSize(mapSize / 4);
        smallRockFieldMask.setSize(mapSize / 4);

        if (hasCivilians) {
            if (!enemyCivilians) {
                baseMask.setSize(mapSize + 1);
                civReclaimMask.randomize(.005f).setSize(mapSize + 1);
                civReclaimMask.intersect(land.copy().minus(unbuildable).deflate(24)).fillCenter(32, false).fillEdge(64, false);
            } else {
                civReclaimMask.setSize(mapSize + 1);
                baseMask.randomize(.005f).setSize(mapSize + 1);
                baseMask.intersect(land.copy().minus(unbuildable).deflate(24)).fillCenter(32, false).fillEdge(32, false).minus(civReclaimMask.copy().inflate(16));
            }
        } else {
            civReclaimMask.setSize(mapSize + 1);
            baseMask.setSize(mapSize + 1);
        }
        allBaseMask.combine(baseMask.copy().inflate(24)).combine(civReclaimMask.copy().inflate(24));

        cliffRockMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .5f + .1f).setSize(mapSize + 1);
        cliffRockMask.intersect(impassable).grow(.5f, SymmetryType.SPAWN, 6).minus(plateaus.copy().outline().inflate(2)).minus(impassable).intersect(land);
        fieldStoneMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .001f).setSize(mapSize + 1);
        fieldStoneMask.intersect(land).minus(impassable).fillEdge(10, false);
        treeMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .2f + .1f).setSize(mapSize / 4);
        treeMask.inflate(2).erode(.5f, SymmetryType.SPAWN).erode(.5f, SymmetryType.SPAWN);
        treeMask.setSize(mapSize + 1);
        treeMask.intersect(land.copy().deflate(8)).minus(impassable.copy().inflate(2)).deflate(2).fillEdge(8, false).minus(notFlat);
        largeRockFieldMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .00075f).fillEdge(32, false).grow(.5f, SymmetryType.SPAWN, 8).setSize(mapSize + 1);
        largeRockFieldMask.minus(unbuildable).intersect(land).minus(impassable.copy().inflate(8));
        smallRockFieldMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .002f).fillEdge(16, false).grow(.5f, SymmetryType.SPAWN, 4).setSize(mapSize + 1);
        smallRockFieldMask.minus(unbuildable).intersect(land).minus(impassable.copy().inflate(8));
    }

    protected void setupWreckPipeline() {
        t1LandWreckMask.setSize(mapSize / 8);
        t2LandWreckMask.setSize(mapSize / 8);
        t3LandWreckMask.setSize(mapSize / 8);
        t2NavyWreckMask.setSize(mapSize / 8);
        navyFactoryWreckMask.setSize(mapSize / 8);
        allWreckMask.setSize(mapSize + 1);

        t1LandWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .0025f).setSize(mapSize + 1);
        t1LandWreckMask.intersect(land).inflate(1).minus(impassable).fillEdge(20, false);
        t2LandWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .002f).setSize(mapSize + 1);
        t2LandWreckMask.intersect(land).minus(impassable).minus(t1LandWreckMask).fillEdge(64, false);
        t3LandWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .0004f).setSize(mapSize + 1);
        t3LandWreckMask.intersect(land).minus(impassable).minus(t1LandWreckMask).minus(t2LandWreckMask).fillEdge(mapSize / 8, false);
        navyFactoryWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .005f).setSize(mapSize + 1);
        navyFactoryWreckMask.intersect(land.copy().inflate(48)).minus(land.copy().inflate(16)).fillEdge(20, false).fillCenter(32, false);
        t2NavyWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .005f).setSize(mapSize + 1);
        t2NavyWreckMask.intersect(land.copy().inflate(4).outline()).fillEdge(20, false);
        allWreckMask.combine(t1LandWreckMask).combine(t2LandWreckMask).combine(t3LandWreckMask).combine(t2NavyWreckMask).inflate(2);
    }

    protected void connectTeams(ConcurrentBinaryMask maskToUse, int minMiddlePoints, int maxMiddlePoints, int numConnections, float maxStepSize) {
        if (numTeams == 0) {
            return;
        }
        List<Spawn> startTeamSpawns = map.getSpawns().stream().filter(spawn -> spawn.getTeamID() == 0).collect(Collectors.toList());
        for (int i = 0; i < numConnections; ++i) {
            Spawn startSpawn = startTeamSpawns.get(random.nextInt(startTeamSpawns.size()));
            int numMiddlePoints;
            if (maxMiddlePoints > minMiddlePoints) {
                numMiddlePoints = random.nextInt(maxMiddlePoints - minMiddlePoints) + minMiddlePoints;
            } else {
                numMiddlePoints = maxMiddlePoints;
            }
            Vector2f start = new Vector2f(startSpawn.getPosition());
            Vector2f end = new Vector2f(start);
            float offCenterAngle = (float) (StrictMath.PI * (1f / 3f + random.nextFloat() / 3f));
            offCenterAngle *= random.nextBoolean() ? 1 : -1;
            offCenterAngle += start.getAngle(new Vector2f(mapSize / 2f, mapSize / 2f));
            end.addPolar(offCenterAngle, random.nextFloat() * mapSize / 4f + mapSize / 4f);
            float maxMiddleDistance = start.getDistance(end);
            maskToUse.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2, (float) (StrictMath.PI / 2), SymmetryType.SPAWN);
        }
    }

    protected void connectTeammates(ConcurrentBinaryMask maskToUse, int maxMiddlePoints, int numConnections, float maxStepSize) {
        if (numTeams == 0) {
            return;
        }
        List<Spawn> startTeamSpawns = map.getSpawns().stream().filter(spawn -> spawn.getTeamID() == 0).collect(Collectors.toList());
        if (startTeamSpawns.size() > 1) {
            startTeamSpawns.forEach(startSpawn -> {
                for (int i = 0; i < numConnections; ++i) {
                    ArrayList<Spawn> otherSpawns = new ArrayList<>(startTeamSpawns);
                    otherSpawns.remove(startSpawn);
                    Spawn endSpawn = otherSpawns.get(random.nextInt(otherSpawns.size()));
                    int numMiddlePoints = random.nextInt(maxMiddlePoints);
                    Vector2f start = new Vector2f(startSpawn.getPosition());
                    Vector2f end = new Vector2f(endSpawn.getPosition());
                    float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
                    maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
                }
            });
        }
    }

    protected void pathInCenterBounds(ConcurrentBinaryMask maskToUse, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        for (int i = 0; i < numPaths; i++) {
            Vector2f start = new Vector2f(random.nextInt(mapSize + 1 - bound * 2) + bound, random.nextInt(mapSize + 1 - bound * 2) + bound);
            Vector2f end = new Vector2f(random.nextInt(mapSize + 1 - bound * 2) + bound, random.nextInt(mapSize + 1 - bound * 2) + bound);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
    }

    protected void pathInEdgeBounds(ConcurrentBinaryMask maskToUse, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        for (int i = 0; i < numPaths; i++) {
            int startX = random.nextInt(bound) + (random.nextBoolean() ? 0 : mapSize - bound);
            int startY = random.nextInt(bound) + (random.nextBoolean() ? 0 : mapSize - bound);
            int endX = random.nextInt(bound * 2) - bound + startX;
            int endY = random.nextInt(bound * 2) - bound + startY;
            Vector2f start = new Vector2f(startX, startY);
            Vector2f end = new Vector2f(endX, endY);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
    }

    protected void pathAroundPoint(ConcurrentBinaryMask maskToUse, Vector2f start, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        for (int i = 0; i < numPaths; i++) {
            int endX = (int) (random.nextFloat() * bound + start.getX());
            int endY = (int) (random.nextFloat() * bound + start.getY());
            Vector2f end = new Vector2f(endX, endY);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
    }

    protected void generateExclusionZones(BinaryMask mask, float spawnSpacing, float mexSpacing, float hydroSpacing) {
        map.getSpawns().forEach(spawn -> mask.fillCircle(spawn.getPosition(), spawnSpacing, true));
        map.getMexes().forEach(mex -> mask.fillCircle(mex.getPosition(), mexSpacing, true));
        map.getHydros().forEach(hydro -> mask.fillCircle(hydro.getPosition(), hydroSpacing, true));
    }

    protected void setupDecalPipeline() {
        fieldDecal.init(land);
        slopeDecal.init(slope, .25f);
        mountainDecal.init(mountains);

        fieldDecal.minus(slopeDecal.copy().inflate(16)).minus(mountainDecal);
    }

    protected void setupResourcePipeline() {
        resourceMask.init(land);
        waterResourceMask.init(land).invert();

        resourceMask.minus(unbuildable).deflate(4);
        resourceMask.fillEdge(16, false).fillCenter(24, false);
        waterResourceMask.minus(unbuildable).deflate(8).fillEdge(16, false).fillCenter(24, false);
    }

    protected void setupTexturePipeline() {
        ConcurrentBinaryMask flat = new ConcurrentBinaryMask(slope, .05f, random.nextLong(), "flat").invert();
        ConcurrentBinaryMask highGround = new ConcurrentBinaryMask(heightmapBase, waterHeight + plateauHeight * 3 / 4f, random.nextLong(), "highGround");
        ConcurrentBinaryMask accentGround = new ConcurrentBinaryMask(land, random.nextLong(), "accentGround");
        ConcurrentBinaryMask accentPlateau = new ConcurrentBinaryMask(plateaus, random.nextLong(), "accentPlateau");
        ConcurrentBinaryMask slopes = new ConcurrentBinaryMask(slope, .15f, random.nextLong(), "slopes");
        ConcurrentBinaryMask accentSlopes = new ConcurrentBinaryMask(slope, .55f, random.nextLong(), "accentSlopes").invert();
        ConcurrentBinaryMask steepHills = new ConcurrentBinaryMask(slope, .55f, random.nextLong(), "steepHills");
        ConcurrentBinaryMask rock = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "rock");
        ConcurrentBinaryMask accentRock = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "accentRock");

        accentGround.minus(highGround).acid(.1f, 0).erode(.4f, SymmetryType.SPAWN).smooth(6, .75f);
        accentPlateau.acid(.1f, 0).erode(.4f, SymmetryType.SPAWN).smooth(6, .75f);
        slopes.flipValues(.95f).erode(.5f, SymmetryType.SPAWN).acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentSlopes.minus(flat).acid(.1f, 0).erode(.5f, SymmetryType.SPAWN).smooth(4, .75f).acid(.55f, 0);
        steepHills.acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentRock.acid(.2f, 0).erode(.3f, SymmetryType.SPAWN).acid(.2f, 0).smooth(2, .5f).intersect(rock);

        accentGroundTexture.init(accentGround, 0, .5f).smooth(12).add(accentGround, .325f).smooth(8).add(accentGround, .25f).clampMax(1f).smooth(2);
        accentPlateauTexture.init(accentPlateau, 0, .5f).smooth(12).add(accentPlateau, .325f).smooth(8).add(accentPlateau, .25f).clampMax(1f).smooth(2);
        slopesTexture.init(slopes, 0, 1).smooth(8).add(slopes, .75f).smooth(4).clampMax(1f);
        accentSlopesTexture.init(accentSlopes, 0, 1).smooth(8).add(accentSlopes, .65f).smooth(4).add(accentSlopes, .5f).smooth(1).clampMax(1f);
        steepHillsTexture.init(steepHills, 0, 1).smooth(8).clampMax(0.35f).add(steepHills, .65f).smooth(4).clampMax(0.65f).add(steepHills, .5f).smooth(1).clampMax(1f);
        waterBeachTexture.init(land.copy().invert().inflate(12).minus(plateaus.copy().minus(ramps)), 0, 1).smooth(12);
        rockTexture.init(rock, 0, 1f).smooth(4).add(rock, 1f).smooth(2).clampMax(1f);
        accentRockTexture.init(accentRock, 0, 1f).smooth(4).clampMax(1f);
    }

    protected void generateExclusionMasks() {
        noProps.init(unbuildable.getFinalMask());
        noBases.init(unbuildable.getFinalMask());
        noCivs.init(unbuildable.getFinalMask());
        noWrecks.init(unbuildable.getFinalMask());

        noProps.combine(allBaseMask.getFinalMask());
        noWrecks.combine(allBaseMask.getFinalMask()).fillCenter(16, true);

        generateExclusionZones(noProps, 30, 1, 8);
        generateExclusionZones(noBases, 128, 32, 32);
        generateExclusionZones(noCivs, 96, 32, 32);
        generateExclusionZones(noWrecks, 128, 8, 32);
    }

    protected void generateAIMarkers() {
        Pipeline.await(passable, passableLand, passableWater);
        long sTime = System.currentTimeMillis();
        CompletableFuture<Void> AmphibiousMarkers = CompletableFuture.runAsync(() -> AIMarkerGenerator.generateAIMarkers(passable.getFinalMask(), map.getAmphibiousAIMarkers(), "AmphPN%d"));
        CompletableFuture<Void> LandMarkers = CompletableFuture.runAsync(() -> AIMarkerGenerator.generateAIMarkers(passableLand.getFinalMask(), map.getLandAIMarkers(), "LandPN%d"));
        CompletableFuture<Void> NavyMarkers = CompletableFuture.runAsync(() -> AIMarkerGenerator.generateAIMarkers(passableWater.getFinalMask(), map.getNavyAIMarkers(), "NavyPN%d"));
        CompletableFuture<Void> AirMarkers = CompletableFuture.runAsync(() -> AIMarkerGenerator.generateAirAIMarkers(map));
        AmphibiousMarkers.join();
        LandMarkers.join();
        NavyMarkers.join();
        AirMarkers.join();
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, generateAIMarkers\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(neroxis.generator.MapGenerator.class));
        }
    }

    protected void addTextures() {
        Pipeline.await(accentGroundTexture, accentPlateauTexture, slopesTexture, accentSlopesTexture, steepHillsTexture, waterBeachTexture, rockTexture, accentRockTexture);
        long sTime = System.currentTimeMillis();
        map.setTextureMasksLowScaled(accentGroundTexture.getFinalMask(), accentPlateauTexture.getFinalMask(), slopesTexture.getFinalMask(), accentSlopesTexture.getFinalMask());
        map.setTextureMasksHighScaled(steepHillsTexture.getFinalMask(), waterBeachTexture.getFinalMask(), rockTexture.getFinalMask(), accentRockTexture.getFinalMask());
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, generateTextures\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(neroxis.generator.MapGenerator.class));
        }
    }

    protected void generateResources() {
        Pipeline.await(resourceMask, plateaus, land, ramps, impassable, unbuildable, allWreckMask, waterResourceMask);
        long sTime = System.currentTimeMillis();
        mexGenerator.generateMexes(resourceMask.getFinalMask(), waterResourceMask.getFinalMask());
        hydroGenerator.generateHydros(resourceMask.getFinalMask().deflate(8));
        generateExclusionMasks();
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, generateResources\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(neroxis.generator.MapGenerator.class));
        }
    }

    protected void generateDecals() {
        Pipeline.await(fieldDecal, slopeDecal, mountainDecal);
        long sTime = System.currentTimeMillis();
        decalGenerator.generateDecals(fieldDecal.getFinalMask(), biome.getDecalMaterials().getFieldNormals(), 32, 32, 32, 64);
        decalGenerator.generateDecals(fieldDecal.getFinalMask(), biome.getDecalMaterials().getFieldAlbedos(), 64, 128, 24, 48);
        decalGenerator.generateDecals(slopeDecal.getFinalMask(), biome.getDecalMaterials().getSlopeNormals(), 16, 32, 16, 32);
        decalGenerator.generateDecals(slopeDecal.getFinalMask(), biome.getDecalMaterials().getSlopeAlbedos(), 64, 128, 32, 48);
        decalGenerator.generateDecals(mountainDecal.getFinalMask(), biome.getDecalMaterials().getMountainNormals(), 32, 32, 32, 64);
        decalGenerator.generateDecals(mountainDecal.getFinalMask(), biome.getDecalMaterials().getMountainAlbedos(), 64, 128, 16, 24);
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, generateDecals\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(neroxis.generator.MapGenerator.class));
        }
    }

    protected void generateProps() {
        Pipeline.await(treeMask, cliffRockMask, largeRockFieldMask, fieldStoneMask);
        long sTime = System.currentTimeMillis();
        propGenerator.generateProps(treeMask.getFinalMask().minus(noProps), biome.getPropMaterials().getTreeGroups(), 3f, 7f);
        propGenerator.generateProps(cliffRockMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), .5f, 3f);
        propGenerator.generateProps(largeRockFieldMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), .5f, 3.5f);
        propGenerator.generateProps(smallRockFieldMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), .5f, 3f);
        propGenerator.generateProps(fieldStoneMask.getFinalMask().minus(noProps), biome.getPropMaterials().getBoulders(), 30f);
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, generateProps\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(neroxis.generator.MapGenerator.class));
        }
    }

    protected void generateUnits() {
        if (!unexplored) {
            Pipeline.await(baseMask, civReclaimMask, t1LandWreckMask, t2LandWreckMask, t3LandWreckMask, t2NavyWreckMask, navyFactoryWreckMask);
            long sTime = System.currentTimeMillis();
            Army army17 = new Army("ARMY_17", new ArrayList<>());
            Group army17Initial = new Group("INITIAL", new ArrayList<>());
            Group army17Wreckage = new Group("WRECKAGE", new ArrayList<>());
            army17.addGroup(army17Initial);
            army17.addGroup(army17Wreckage);
            Army civilian = new Army("NEUTRAL_CIVILIAN", new ArrayList<>());
            Group civilianInitial = new Group("INITIAL", new ArrayList<>());
            civilian.addGroup(civilianInitial);
            map.addArmy(army17);
            map.addArmy(civilian);
            try {
                unitGenerator.generateBases(baseMask.getFinalMask().minus(noBases), UnitGenerator.MEDIUM_ENEMY, army17, army17Initial, 512f);
                unitGenerator.generateBases(civReclaimMask.getFinalMask().minus(noCivs), UnitGenerator.MEDIUM_RECLAIM, civilian, civilianInitial, 256f);
            } catch (IOException e) {
                generationComplete = false;
                System.out.println("Could not generate bases due to lua parsing error");
                e.printStackTrace();
            }
            unitGenerator.generateUnits(t1LandWreckMask.getFinalMask().minus(noWrecks), UnitGenerator.T1_Land, army17, army17Wreckage, 1f, 4f);
            unitGenerator.generateUnits(t2LandWreckMask.getFinalMask().minus(noWrecks), UnitGenerator.T2_Land, army17, army17Wreckage, 30f);
            unitGenerator.generateUnits(t3LandWreckMask.getFinalMask().minus(noWrecks), UnitGenerator.T3_Land, army17, army17Wreckage, 192f);
            unitGenerator.generateUnits(t2NavyWreckMask.getFinalMask().minus(noWrecks), UnitGenerator.T2_Navy, army17, army17Wreckage, 128f);
            unitGenerator.generateUnits(navyFactoryWreckMask.getFinalMask().minus(noWrecks), UnitGenerator.Navy_Factory, army17, army17Wreckage, 256f);
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateBases\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(neroxis.generator.MapGenerator.class));
            }
        }
    }

    protected void setHeightmap() {
        Pipeline.await(heightmapBase);
        long sTime = System.currentTimeMillis();
        map.setHeightImage(heightmapBase.getFinalMask());
        map.getHeightmap().getRaster().setPixel(0, 0, new int[]{0});
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, setHeightmap\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(neroxis.generator.MapGenerator.class));
        }
    }

    protected void setHeights() {
        long sTime = System.currentTimeMillis();
        map.setHeights();
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, setPlacements\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(neroxis.generator.MapGenerator.class));
        }
    }
}

