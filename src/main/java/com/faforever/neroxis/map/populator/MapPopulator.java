package com.faforever.neroxis.map.populator;

import com.faforever.neroxis.map.*;
import com.faforever.neroxis.map.exporter.MapExporter;
import com.faforever.neroxis.map.generator.placement.*;
import com.faforever.neroxis.map.importer.MapImporter;
import com.faforever.neroxis.map.mask.BooleanMask;
import com.faforever.neroxis.map.mask.FloatMask;
import com.faforever.neroxis.map.mask.Vector4Mask;
import com.faforever.neroxis.util.ArgumentParser;
import com.faforever.neroxis.util.FileUtils;
import com.faforever.neroxis.util.Util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public strictfp class MapPopulator {

    private Path inMapPath;
    private Path outFolderPath;
    private SCMap map;
    private Path propsPath;
    private boolean populateSpawns;
    private boolean populateMexes;
    private boolean populateHydros;
    private boolean populateProps;
    private boolean populateAI;
    private int spawnCount;
    private int mexCountPerPlayer;
    private int hydroCountPerPlayer;
    private boolean populateTextures;
    private int mapImageSize;
    private boolean erosionNormal;
    private int erosionResolution;
    private boolean keepLayer1;
    private boolean keepLayer2;
    private boolean keepLayer3;
    private boolean keepLayer4;
    private boolean keepLayer5;
    private boolean keepLayer6;
    private boolean keepLayer7;
    private boolean keepLayer8;

    private BooleanMask resourceMask;
    private BooleanMask waterResourceMask;

    private SymmetrySettings symmetrySettings;

    public static void main(String[] args) throws Exception {

        Locale.setDefault(Locale.ROOT);

        MapPopulator populator = new MapPopulator();

        populator.interpretArguments(args);
        if (populator.inMapPath == null) {
            return;
        }

        System.out.println("Populating map " + populator.inMapPath);
        populator.importMap();
        populator.populate();
        populator.exportMap();
        System.out.println("Saving map to " + populator.outFolderPath.toAbsolutePath());
        System.out.println("Terrain Symmetry: " + populator.symmetrySettings.getTerrainSymmetry());
        System.out.println("Team Symmetry: " + populator.symmetrySettings.getTeamSymmetry());
        System.out.println("Spawn Symmetry: " + populator.symmetrySettings.getSpawnSymmetry());
        System.out.println("Done");
    }

    public void interpretArguments(String[] args) {
        interpretArguments(ArgumentParser.parse(args));
    }

    private void interpretArguments(Map<String, String> arguments) {
        if (arguments.containsKey("help")) {
            System.out.println("map-populator usage:\n" +
                    "--help                 produce help message\n" +
                    "--in-folder-path arg   required, set the input folder for the map\n" +
                    "--out-folder-path arg  required, set the output folder for the populated map\n" +
                    "--team-symmetry arg    required, set the symmetry for the teams(X, Z, XZ, ZX)\n" +
                    "--spawn-symmetry arg   required, set the symmetry for the spawns(POINT, X, Z, XZ, ZX)\n" +
                    "--spawns arg           optional, populate arg spawns\n" +
                    "--mexes arg            optional, populate arg mexes per player\n" +
                    "--hydros arg           optional, populate arg hydros per player\n" +
                    "--props arg            optional, populate props arg is the path to the props json\n" +
                    "--wrecks               optional, populate wrecks\n" +
                    "--textures arg         optional, populate textures arg determines which layers are populated (1, 2, 3, 4, 5, 6, 7, 8)\n" +
                    " - ie: to populate all texture layers except layer 7, use: --textures 1234568\n" +
                    " - texture  layers 1-8 are: 1 Accent Ground, 2 Accent Plateaus, 3 Slopes, 4 Accent Slopes, 5 Steep Hills, 6 Water/Beach, 7 Rock, 8 Accent Rock" +
                    "--decals               optional, populate decals\n" +
                    "--erosion arg          optional, add map wide erosion normal at arg resolution\n" +
                    "--ai                   optional, populate ai markers\n" +
                    "--keep-current-decals  optional, prevents decals currently on the map from being deleted\n" +
                    "--debug                optional, turn on debugging options\n");
            return;
        }

        if (arguments.containsKey("debug")) {
            Util.DEBUG = true;
        }

        if (!arguments.containsKey("in-folder-path")) {
            System.out.println("Input Folder not Specified");
            return;
        }

        if (!arguments.containsKey("out-folder-path")) {
            System.out.println("Output Folder not Specified");
            return;
        }

        if (!arguments.containsKey("team-symmetry") || !arguments.containsKey("spawn-symmetry")) {
            System.out.println("Symmetries not Specified");
            return;
        }

        inMapPath = Paths.get(arguments.get("in-folder-path"));
        outFolderPath = Paths.get(arguments.get("out-folder-path"));
        symmetrySettings = new SymmetrySettings(Symmetry.valueOf(arguments.get("spawn-symmetry")), Symmetry.valueOf(arguments.get("team-symmetry")), Symmetry.valueOf(arguments.get("spawn-symmetry")));
        populateSpawns = arguments.containsKey("spawns");
        if (populateSpawns) {
            spawnCount = Integer.parseInt(arguments.get("spawns"));
        }
        populateMexes = arguments.containsKey("mexes");
        if (populateMexes) {
            mexCountPerPlayer = Integer.parseInt(arguments.get("mexes"));
        }
        populateHydros = arguments.containsKey("hydros");
        if (populateHydros) {
            hydroCountPerPlayer = Integer.parseInt(arguments.get("hydros"));
        }
        populateProps = arguments.containsKey("props");
        if (populateProps) {
            propsPath = Paths.get(arguments.get("props"));
        }
        erosionNormal = arguments.containsKey("erosion");
        if (erosionNormal) {
            erosionResolution = Integer.parseInt(arguments.get("erosion"));
        }
        populateTextures = arguments.containsKey("textures");
        if (populateTextures) {
            String whichTextures = arguments.get("textures");
            if (whichTextures != null) {
                keepLayer1 = !whichTextures.contains("1");
                keepLayer2 = !whichTextures.contains("2");
                keepLayer3 = !whichTextures.contains("3");
                keepLayer4 = !whichTextures.contains("4");
                keepLayer5 = !whichTextures.contains("5");
                keepLayer6 = !whichTextures.contains("6");
                keepLayer7 = !whichTextures.contains("7");
                keepLayer8 = !whichTextures.contains("8");
            }
        }
        if (arguments.containsKey("texture-res")) {
            mapImageSize = Integer.parseInt(arguments.get("texture-res")) / 128 * 128;
        }
        populateAI = arguments.containsKey("ai");
    }

    public void importMap() {
        try {
            map = MapImporter.importMap(inMapPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while importing the map.");
        }
    }

    public void exportMap() {
        long startTime = System.currentTimeMillis();
        MapExporter.exportMap(outFolderPath, map, true, false);
        System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);
    }

    public void populate() throws Exception {
        /*SupComSlopeValues
        const float FlatHeight = 0.002f;
        const float NonFlatHeight = 0.30f;
        const float AlmostUnpassableHeight = 0.75f;
        const float UnpassableHeight = 0.75f;
        const float ScaleHeight = 256;
        */

        Random random = new Random();
        boolean waterPresent = map.getBiome().getWaterSettings().isWaterPresent();
        FloatMask heightmapBase = new FloatMask(map.getHeightmap(), random.nextLong(), symmetrySettings, map.getHeightMapScale(), "heightmapBase");
        heightmapBase = new FloatMask(heightmapBase, random.nextLong());
        heightmapBase.applySymmetry(SymmetryType.SPAWN);
        heightmapBase.writeToImage(map.getHeightmap(), 1 / map.getHeightMapScale());
        float waterHeight;
        if (waterPresent) {
            waterHeight = map.getBiome().getWaterSettings().getElevation();
        } else {
            waterHeight = heightmapBase.getMin();
        }

        BooleanMask land = new BooleanMask(heightmapBase, waterHeight, random.nextLong());
        BooleanMask plateaus = new BooleanMask(heightmapBase, waterHeight + 3f, random.nextLong());
        FloatMask slope = new FloatMask(heightmapBase, random.nextLong()).gradient();
        BooleanMask impassable = new BooleanMask(slope, .9f, random.nextLong());
        BooleanMask ramps = new BooleanMask(slope, .25f, random.nextLong()).subtract(impassable);
        BooleanMask passable = impassable.copy().invert();
        BooleanMask passableLand = new BooleanMask(land, random.nextLong());
        BooleanMask passableWater = new BooleanMask(land, random.nextLong()).invert();

        if (populateSpawns) {
            if (spawnCount > 0) {
                SpawnPlacer spawnPlacer = new SpawnPlacer(map, random.nextLong());
                float spawnSeparation = StrictMath.max(random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16, 24);
                BooleanMask spawns = land.copy();
                spawns.multiply(passable).subtract(ramps).deflate(16);
                spawnPlacer.placeSpawns(spawnCount, spawns, spawnSeparation);
            } else {
                map.getSpawns().clear();
            }
        }

        if (populateMexes || populateHydros) {
            resourceMask = new BooleanMask(land, random.nextLong());
            waterResourceMask = new BooleanMask(land, random.nextLong()).invert();

            resourceMask.subtract(impassable).deflate(8).subtract(ramps);
            resourceMask.fillEdge(16, false).fillCenter(16, false);
            waterResourceMask.subtract(ramps).deflate(16).fillEdge(16, false).fillCenter(16, false);
        }

        if (populateMexes) {
            if (mexCountPerPlayer > 0) {
                MexPlacer mexPlacer = new MexPlacer(map, random.nextLong());

                mexPlacer.placeMexes(mexCountPerPlayer * map.getSpawnCount(), resourceMask, waterResourceMask);
            } else {
                map.getMexes().clear();
            }
        }

        if (populateHydros) {
            if (hydroCountPerPlayer > 0) {
                HydroPlacer hydroPlacer = new HydroPlacer(map, random.nextLong());

                hydroPlacer.placeHydros(hydroCountPerPlayer * map.getSpawnCount(), resourceMask.deflate(4));
            } else {
                map.getHydros().clear();
            }
        }

        if (populateTextures) {

            int smallWaterSizeLimit = 9000;

            FloatMask[] textureMasksLow = new Vector4Mask(map.getTextureMasksLow(), random.nextLong(), symmetrySettings, 1f, "TextureMasksLow")
                    .subtractScalar(128f).divideScalar(127f).clampComponentMin(0f).clampComponentMax(1f).splitComponentMasks();
            FloatMask[] textureMasksHigh = new Vector4Mask(map.getTextureMasksHigh(), random.nextLong(), symmetrySettings, 1f, "TextureMasksHigh")
                    .subtractScalar(128f).divideScalar(127f).clampComponentMin(0f).clampComponentMax(1f).splitComponentMasks();

            if (mapImageSize == 0 || mapImageSize > map.getSize()) {
                mapImageSize = map.getSize();
            }

            map.setTextureMasksLow(new BufferedImage(mapImageSize, mapImageSize, BufferedImage.TYPE_INT_ARGB));
            map.setTextureMasksHigh(new BufferedImage(mapImageSize, mapImageSize, BufferedImage.TYPE_INT_ARGB));

            BooleanMask water = new BooleanMask(land.copy().invert(), random.nextLong());
            BooleanMask flat = new BooleanMask(slope, .05f, random.nextLong()).invert();
            BooleanMask inland = new BooleanMask(land, random.nextLong());
            BooleanMask highGround = new BooleanMask(heightmapBase, waterHeight + 3f, random.nextLong());
            BooleanMask aboveBeach = new BooleanMask(heightmapBase, waterHeight + 1.5f, random.nextLong());
            BooleanMask aboveBeachEdge = new BooleanMask(heightmapBase, waterHeight + 3f, random.nextLong());
            BooleanMask flatAboveCoast = new BooleanMask(heightmapBase, waterHeight + 0.29f, random.nextLong());
            BooleanMask higherFlatAboveCoast = new BooleanMask(heightmapBase, waterHeight + 1.2f, random.nextLong());
            BooleanMask lowWaterBeach = new BooleanMask(heightmapBase, waterHeight, random.nextLong());
            BooleanMask tinyWater = water.copy().removeAreasBiggerThan(StrictMath.min(smallWaterSizeLimit / 4 + 750, smallWaterSizeLimit * 2 / 3));
            BooleanMask smallWater = water.copy().removeAreasBiggerThan(smallWaterSizeLimit);
            BooleanMask smallWaterBeach = smallWater.copy().subtract(tinyWater).inflate(2).add(tinyWater);
            FloatMask smallWaterBeachTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);

            inland.deflate(2);
            flatAboveCoast.multiply(flat);
            higherFlatAboveCoast.multiply(flat);
            lowWaterBeach.invert().subtract(smallWater).inflate(6).subtract(aboveBeach);
            smallWaterBeach.subtract(flatAboveCoast).blur(2, 0.5f).subtract(aboveBeach).subtract(higherFlatAboveCoast).blur(1);
            smallWaterBeach.setSize(mapImageSize);

            smallWaterBeachTexture.init(smallWaterBeach, 0f, 1f).blur(8).clampMax(0.35f).add(smallWaterBeach, 1f).blur(4).clampMax(0.65f).add(smallWaterBeach, 1f).blur(1).add(smallWaterBeach, 1f).clampMax(1f);

            BooleanMask waterBeach = new BooleanMask(heightmapBase, waterHeight + 1f, random.nextLong());
            BooleanMask accentGround = new BooleanMask(land, random.nextLong());
            BooleanMask accentPlateau = new BooleanMask(plateaus, random.nextLong());
            BooleanMask slopes = new BooleanMask(slope, .1f, random.nextLong());
            BooleanMask accentSlopes = new BooleanMask(slope, .75f, random.nextLong()).invert();
            BooleanMask steepHills = new BooleanMask(slope, .55f, random.nextLong());
            BooleanMask rock = new BooleanMask(slope, 1.25f, random.nextLong());
            BooleanMask accentRock = new BooleanMask(slope, 1.25f, random.nextLong());
            FloatMask waterBeachTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentGroundTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentPlateauTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask slopesTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentSlopesTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask steepHillsTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask rockTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentRockTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);

            accentGround.subtract(highGround).acid(.05f, 0).erode(.85f).blur(2, .75f).acid(.45f, 0);
            accentPlateau.acid(.05f, 0).erode(.85f).blur(2, .75f).acid(.45f, 0);
            slopes.multiply(land).flipValues(.95f).erode(.5f).acid(.3f, 0).erode(.2f);
            accentSlopes.subtract(flat).multiply(land).acid(.1f, 0).erode(.5f).blur(4, .75f).acid(.55f, 0);
            steepHills.acid(.3f, 0).erode(.2f);
            if (waterPresent) {
                waterBeach.invert().subtract(smallWater).subtract(flatAboveCoast).subtract(inland).inflate(1).add(lowWaterBeach).blur(5, 0.5f).subtract(aboveBeach).subtract(higherFlatAboveCoast).blur(2).blur(1);
            } else {
                waterBeach.clear();
            }
            accentRock.acid(.2f, 0).erode(.3f).acid(.2f, 0).blur(2, .5f).multiply(rock);

            accentGround.setSize(mapImageSize);
            accentPlateau.setSize(mapImageSize);
            slopes.setSize(mapImageSize);
            accentSlopes.setSize(mapImageSize);
            steepHills.setSize(mapImageSize);
            waterBeach.setSize(mapImageSize);
            aboveBeachEdge.setSize(mapImageSize);
            rock.setSize(mapImageSize);
            accentRock.setSize(mapImageSize);
            smallWater.setSize(mapImageSize);
            accentGroundTexture.init(accentGround, 0f, 1f).blur(8).add(accentGround, .65f).blur(4).add(accentGround, .5f).blur(1).clampMax(1f);
            accentPlateauTexture.init(accentPlateau, 0f, 1f).blur(8).add(accentPlateau, .65f).blur(4).add(accentPlateau, .5f).blur(1).clampMax(1f);
            slopesTexture.init(slopes, 0f, 1f).blur(8).add(slopes, .65f).blur(4).add(slopes, .5f).blur(1).clampMax(1f);
            accentSlopesTexture.init(accentSlopes, 0f, 1f).blur(8).add(accentSlopes, .65f).blur(4).add(accentSlopes, .5f).blur(1).clampMax(1f);
            steepHillsTexture.init(steepHills, 0f, 1f).blur(8).clampMax(0.35f).add(steepHills, .65f).blur(4).clampMax(0.65f).add(steepHills, .5f).blur(1).add(steepHills, 1f).clampMax(1f);
            waterBeachTexture.init(waterBeach, 0f, 1f).subtract(rock, 1f).subtract(aboveBeachEdge, 1f).clampMin(0f).blur(2, rock.copy().invert()).add(waterBeach, 1f).subtract(rock, 1f);
            waterBeachTexture.subtract(aboveBeachEdge, .9f).clampMin(0f).blur(2, rock.copy().invert()).subtract(rock, 1f).subtract(aboveBeachEdge, .8f).clampMin(0f).add(waterBeach, .65f).blur(2, rock.copy().invert());
            waterBeachTexture.subtract(rock, 1f).subtract(aboveBeachEdge, 0.7f).clampMin(0f).add(waterBeach, .5f).blur(2, rock.copy().invert()).blur(2, rock.copy().invert()).subtract(rock, 1f).clampMin(0f).blur(2, rock.copy().invert());
            waterBeachTexture.blur(2, rock.copy().invert()).subtract(rock, 1f).clampMin(0f).blur(2, rock.copy().invert()).blur(1, rock.copy().invert()).blur(1, rock.copy().invert()).clampMax(1f);
            waterBeachTexture.removeAreasOfSpecifiedSizeWithLocalMaximums(0, smallWaterSizeLimit, 15, 1f).blur(1).blur(1);
            waterBeachTexture.add(smallWaterBeachTexture).clampMax(1f);
            rockTexture.init(rock, 0f, 1f).blur(8).clampMax(0.2f).add(rock, .65f).blur(4).clampMax(0.3f).add(rock, .5f).blur(1).add(rock, 1f).clampMax(1f);
            accentRockTexture.init(accentRock, 0f, 1f).clampMin(0f).blur(8).add(accentRock, .65f).blur(4).add(accentRock, .5f).blur(1).clampMax(1f);

            if (keepLayer1) {
                accentGroundTexture = new FloatMask(textureMasksLow[0], null);
            }
            if (keepLayer2) {
                accentPlateauTexture = new FloatMask(textureMasksLow[1], null);
            }
            if (keepLayer3) {
                slopesTexture = new FloatMask(textureMasksLow[2], null);
            }
            if (keepLayer4) {
                accentSlopesTexture = new FloatMask(textureMasksLow[3], null);
            }
            if (keepLayer5) {
                steepHillsTexture = new FloatMask(textureMasksHigh[4], null);
            }
            if (keepLayer6) {
                waterBeachTexture = new FloatMask(textureMasksHigh[5], null);
            }
            if (keepLayer7) {
                rockTexture = new FloatMask(textureMasksHigh[6], null);
            }
            if (keepLayer8) {
                accentRockTexture = new FloatMask(textureMasksHigh[7], null);
            }

            map.setTextureMasksScaled(map.getTextureMasksLow(), accentGroundTexture, accentPlateauTexture, slopesTexture, accentSlopesTexture);
            map.setTextureMasksScaled(map.getTextureMasksHigh(), steepHillsTexture, waterBeachTexture, rockTexture, accentRockTexture);
        }

        if (erosionNormal) {
            FloatMask erosionHeightMask = heightmapBase.resample(erosionResolution);
        }

        if (populateProps) {
            map.getProps().clear();
            PropPlacer propPlacer = new PropPlacer(map, random.nextLong());
            PropMaterials propMaterials;

            try {
                propMaterials = FileUtils.deserialize(propsPath.toString(), PropMaterials.class);
            } catch (IOException e) {
                throw new Exception("An error occured while loading props\n", e);
            }

            BooleanMask flatEnough = new BooleanMask(slope, .02f, random.nextLong());
            BooleanMask flatish = new BooleanMask(slope, .042f, random.nextLong());
            BooleanMask nearSteepHills = new BooleanMask(slope, .55f, random.nextLong());
            BooleanMask flatEnoughNearRock = new BooleanMask(slope, 1.25f, random.nextLong());
            flatEnough.invert().multiply(land).erode(.15f);
            flatish.invert().multiply(land).erode(.15f);
            nearSteepHills.inflate(6).add(flatEnoughNearRock.copy().inflate(16).multiply(nearSteepHills.copy().inflate(11).subtract(flatEnough))).multiply(land.copy().deflate(10));
            flatEnoughNearRock.inflate(7).add(nearSteepHills).multiply(flatish);

            BooleanMask treeMask = new BooleanMask(flatEnough, random.nextLong());
            BooleanMask cliffRockMask = new BooleanMask(land, random.nextLong());
            BooleanMask fieldStoneMask = new BooleanMask(land, random.nextLong());
            BooleanMask largeRockFieldMask = new BooleanMask(land, random.nextLong());
            BooleanMask smallRockFieldMask = new BooleanMask(land, random.nextLong());

            treeMask.deflate(6).erode(0.5f).multiply(land.copy().deflate(15).acid(.05f, 0).erode(.85f).blur(2, .75f).acid(.45f, 0));
            cliffRockMask.randomize(.017f).multiply(flatEnoughNearRock);
            fieldStoneMask.randomize(.00145f).multiply(flatEnoughNearRock.copy().deflate(1));
            largeRockFieldMask.randomize(.015f).multiply(flatEnoughNearRock);
            smallRockFieldMask.randomize(.015f).multiply(flatEnoughNearRock);

            BooleanMask noProps = new BooleanMask(impassable, null);

            for (int i = 0; i < map.getSpawnCount(); i++) {
                noProps.fillCircle(map.getSpawn(i).getPosition(), 30, true);
            }
            for (int i = 0; i < map.getMexCount(); i++) {
                noProps.fillCircle(map.getMex(i).getPosition(), 10, true);
            }
            for (int i = 0; i < map.getHydroCount(); i++) {
                noProps.fillCircle(map.getHydro(i).getPosition(), 16, true);
            }

            if (propMaterials.getTreeGroups() != null && propMaterials.getTreeGroups().length > 0) {
                propPlacer.placeProps(treeMask.subtract(noProps), propMaterials.getTreeGroups(), 3f);
            }
            if (propMaterials.getRocks() != null && propMaterials.getRocks().length > 0) {
                propPlacer.placeProps(cliffRockMask.subtract(noProps), propMaterials.getRocks(), 1.5f);
                propPlacer.placeProps(largeRockFieldMask.subtract(noProps), propMaterials.getRocks(), 1.5f);
                propPlacer.placeProps(smallRockFieldMask.subtract(noProps), propMaterials.getRocks(), 1.5f);
            }
            if (propMaterials.getBoulders() != null && propMaterials.getBoulders().length > 0) {
                propPlacer.placeProps(fieldStoneMask.subtract(noProps), propMaterials.getBoulders(), 30f);
            }
        }

        if (populateAI) {
            map.getAmphibiousAIMarkers().clear();
            map.getLandAIMarkers().clear();
            map.getNavyAIMarkers().clear();
            map.getAirAIMarkers().clear();
            BooleanMask passableAI = passable.copy().add(land.copy().invert()).fillEdge(8, false);
            passableLand.multiply(passableAI);
            passableWater.deflate(16).multiply(passableAI).fillEdge(8, false);
            AIMarkerPlacer.placeAIMarkers(passableAI, map.getAmphibiousAIMarkers(), "AmphPN%d");
            AIMarkerPlacer.placeAIMarkers(passableLand, map.getLandAIMarkers(), "LandPN%d");
            AIMarkerPlacer.placeAIMarkers(passableWater, map.getNavyAIMarkers(), "NavyPN%d");
            AIMarkerPlacer.placeAirAIMarkers(map);
        }

        map.setHeights();
    }
}
