package neroxis.generator.mapstyles;

import neroxis.brushes.Brushes;
import neroxis.generator.*;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public strictfp class ValleyStyleGenerator extends BaseStyleGenerator {

    private ConcurrentBinaryMask t1LandWreckMask;
    private ConcurrentBinaryMask t2LandWreckMask;
    private ConcurrentBinaryMask t3LandWreckMask;
    private ConcurrentBinaryMask t2NavyWreckMask;
    private ConcurrentBinaryMask navyFactoryWreckMask;
    private ConcurrentBinaryMask allWreckMask;
    private ConcurrentBinaryMask treeMask;
    private ConcurrentBinaryMask cliffRockMask;
    private ConcurrentBinaryMask fieldStoneMask;
    private ConcurrentBinaryMask largeRockFieldMask;
    private ConcurrentBinaryMask smallRockFieldMask;
    private ConcurrentBinaryMask baseMask;
    private ConcurrentBinaryMask civReclaimMask;
    private ConcurrentBinaryMask allBaseMask;
    private BinaryMask noProps;
    private BinaryMask noWrecks;
    private BinaryMask noBases;
    private BinaryMask noCivs;

    public ValleyStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
    }

    public SCMap generate() {
        hasCivilians = random.nextBoolean() && !unexplored;
        enemyCivilians = random.nextBoolean() && hasCivilians;

        SpawnGenerator spawnGenerator = new SpawnGenerator(map, random.nextLong());
        MexGenerator mexGenerator = new MexGenerator(map, random.nextLong());
        HydroGenerator hydroGenerator = new HydroGenerator(map, random.nextLong());
        PropGenerator propGenerator = new PropGenerator(map, random.nextLong());
        DecalGenerator decalGenerator = new DecalGenerator(map, random.nextLong());
        UnitGenerator unitGenerator = new UnitGenerator(random.nextLong());

        int spawnSeparation = random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16;

        BinaryMask[] spawnMasks = spawnGenerator.generateSpawns(spawnSeparation, symmetrySettings, plateauDensity, spawnSize);
        spawnLandMask.init(spawnMasks[0]);
        spawnPlateauMask.init(spawnMasks[1]);

        setupPipeline();

        random = null;
        Pipeline.start();

        CompletableFuture<Void> aiMarkerFuture = CompletableFuture.runAsync(() -> {
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
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        CompletableFuture<Void> textureFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(accentGroundTexture, accentPlateauTexture, slopesTexture, accentSlopesTexture, steepHillsTexture, waterBeachTexture, rockTexture, accentRockTexture);
            long sTime = System.currentTimeMillis();
            map.setTextureMasksLowScaled(accentGroundTexture.getFinalMask(), accentPlateauTexture.getFinalMask(), slopesTexture.getFinalMask(), accentSlopesTexture.getFinalMask());
            map.setTextureMasksHighScaled(steepHillsTexture.getFinalMask(), waterBeachTexture.getFinalMask(), rockTexture.getFinalMask(), accentRockTexture.getFinalMask());
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateTextures\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        CompletableFuture<Void> resourcesFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(resourceMask, plateaus, land, ramps, impassable, unbuildable, allWreckMask, waterResourceMask);
            long sTime = System.currentTimeMillis();
            mexGenerator.generateMexes(resourceMask.getFinalMask(), waterResourceMask.getFinalMask());
            hydroGenerator.generateHydros(resourceMask.getFinalMask().deflate(8));
            generateExclusionMasks();
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, generateResources\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        CompletableFuture<Void> decalsFuture = CompletableFuture.runAsync(() -> {
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
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        resourcesFuture.join();

        CompletableFuture<Void> propsFuture = CompletableFuture.runAsync(() -> {
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
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        CompletableFuture<Void> unitsFuture = CompletableFuture.runAsync(() -> {
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
                            Util.getStackTraceLineInClass(MapGenerator.class));
                }
            }
        });

        CompletableFuture<Void> heightMapFuture = CompletableFuture.runAsync(() -> {
            Pipeline.await(heightmapBase);
            long sTime = System.currentTimeMillis();
            map.setHeightImage(heightmapBase.getFinalMask());
            map.getHeightmap().getRaster().setPixel(0, 0, new int[]{0});
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, setHeightmap\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

        propsFuture.join();
        decalsFuture.join();
        aiMarkerFuture.join();
        heightMapFuture.join();
        unitsFuture.join();

        CompletableFuture<Void> placementFuture = CompletableFuture.runAsync(() -> {
            long sTime = System.currentTimeMillis();
            map.setHeights();
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, setPlacements\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(MapGenerator.class));
            }
        });

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

    private void setupTerrainPipeline() {
        teamConnectionsInit();
        landInit();
        plateausInit();
        mountainInit();

        addSpawnTerrain();
    }

    private void teamConnectionsInit() {
        float maxStepSize = mapSize / 128f;
        int minMiddlePoints = 0;
        int maxMiddlePoints = 1;
        int numTeamConnections = (int) ((rampDensity + plateauDensity + (1 - mountainDensity)) / 3 * 2 + 1 + spawnCount / 4);
        int numTeammateConnections = 1;
        connections.setSize(mapSize + 1);

        connectTeams(connections, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize);
        connectTeammates(connections, maxMiddlePoints, numTeammateConnections, maxStepSize);
    }

    private void landInit() {
        land.setSize(mapSize + 1);
        land.invert();
    }

    private void plateausInit() {
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 16;
        int numPaths = (int) (12 * plateauDensity) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = 0;
        plateaus.setSize(mapSize + 1);

        pathInCenterBounds(plateaus, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        plateaus.inflate(mapSize / 256f).setSize(mapSize / 4);
        plateaus.grow(.5f, SymmetryType.TERRAIN, 4).setSize(mapSize + 1);
        plateaus.smooth(12);
    }

    private void mountainInit() {
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 8;
        int numPaths = (int) (8 + 8 * (1 - mountainDensity) / symmetrySettings.getTerrainSymmetry().getNumSymPoints());
        int bound = (int) (mapSize / 16 * (3 * (random.nextFloat() * .25f + mountainDensity * .75f) + 1));
        mountains.setSize(mapSize + 1);
        ConcurrentBinaryMask noMountains = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "noMountains");

        pathInCenterBounds(noMountains, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        noMountains.setSize(mapSize / 4);
        noMountains.grow(.5f, SymmetryType.SPAWN, (int) (maxStepSize * 2)).setSize(mapSize + 1);
        noMountains.smooth(mapSize / 64);

        mountains.invert().minus(noMountains);
    }

    private void addSpawnTerrain() {
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
            land.erode(.5f, SymmetryType.SPAWN, 12).combine((ConcurrentBinaryMask) spawnLandMask.copy().setSize(mapSize / 8)).combine((ConcurrentBinaryMask) spawnPlateauMask.copy().setSize(mapSize / 8))
                    .smooth(4, .75f).grow(.5f, SymmetryType.SPAWN, 4).setSize(mapSize + 1);
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

    private void initRamps() {
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

    private void setupHeightmapPipeline() {
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

        float mountainBrushDensity = mapSize < 512 ? .1f : .05f;

        heightmapMountains.useBrushWithinAreaWithDensity(mountains, brush3, mountainBrushSize, mountainBrushDensity, 14f, false);

        ConcurrentBinaryMask paintedMountains = new ConcurrentBinaryMask(heightmapMountains, plateauHeight / 2, random.nextLong(), "paintedMountains");

        mountains.replace(paintedMountains);
        land.combine(paintedMountains);

        heightmapMountains.smooth(4, mountains.copy().inflate(32).minus(mountains.copy().inflate(4)));

        heightmapPlateaus.useBrushWithinAreaWithDensity(plateaus, brush1, plateauBrushSize, .64f, 8f, false).clampMax(plateauHeight);

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

        heightmapValleys.useBrushWithinAreaWithDensity(valleys, brush2, smallFeatureBrushSize, .72f, -0.35f, false)
                .clampMin(valleyFloor);
        heightmapHills.useBrushWithinAreaWithDensity(hills.combine(mountains.copy().outline().inflate(4).acid(.01f, 4)), brush4, smallFeatureBrushSize, .72f, 0.5f, false);

        initRamps();

        ConcurrentBinaryMask water = land.copy().invert();
        ConcurrentBinaryMask deepWater = water.copy().deflate(32);

        heightmapOcean.addDistance(land, -.45f).clampMin(oceanFloor).useBrushWithinAreaWithDensity(water.deflate(8).minus(deepWater), brush5, 24, 1f, .5f, false)
                .useBrushWithinAreaWithDensity(deepWater, brush5, 64, .065f, 1f, false).clampMax(0).smooth(4, deepWater).smooth(1);

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

    private void setupPropPipeline() {
        baseMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "base");
        civReclaimMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "civReclaim");
        allBaseMask = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "allBase");
        treeMask = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetrySettings, "tree");
        cliffRockMask = new ConcurrentBinaryMask(mapSize / 16, random.nextLong(), symmetrySettings, "cliffRock");
        fieldStoneMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "fieldStone");
        largeRockFieldMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "largeRockField");
        smallRockFieldMask = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "smallRockField");

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

    private void setupWreckPipeline() {
        t1LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t1LandWreck");
        t2LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t2LandWreck");
        t3LandWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t3LandWreck");
        t2NavyWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "t2NavyWreck");
        navyFactoryWreckMask = new ConcurrentBinaryMask(mapSize / 8, random.nextLong(), symmetrySettings, "navyFactoryWreck");
        allWreckMask = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "allWreck");

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

    private void generateExclusionMasks() {
        noProps = new BinaryMask(unbuildable.getFinalMask(), null);
        noBases = new BinaryMask(unbuildable.getFinalMask(), null);
        noCivs = new BinaryMask(unbuildable.getFinalMask(), null);
        noWrecks = new BinaryMask(unbuildable.getFinalMask(), null);

        noProps.combine(allBaseMask.getFinalMask());
        noWrecks.combine(allBaseMask.getFinalMask()).fillCenter(16, true);

        generateExclusionZones(noProps, 30, 1, 8);
        generateExclusionZones(noBases, 128, 32, 32);
        generateExclusionZones(noCivs, 96, 32, 32);
        generateExclusionZones(noWrecks, 128, 8, 32);
    }
}

