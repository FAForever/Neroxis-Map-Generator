package populator;

import exporter.SCMapExporter;
import exporter.SaveExporter;
import exporter.ScenarioExporter;
import generator.*;
import importer.SCMapImporter;
import importer.SaveImporter;
import map.*;
import util.ArgumentParser;
import util.FileUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public strictfp class MapPopulator {

    public static boolean DEBUG = false;

    private Path inMapPath;
    private Path outFolderPath;
    private String mapFolder;
    private String mapName;
    private SCMap map;
    private Path propsPath;
    private boolean populateSpawns;
    private boolean populateMexes;
    private boolean populateHydros;
    private boolean populateProps;
    private boolean populateAI;
    private boolean populateDecals;
    private int spawnCount;
    private int mexCountPerPlayer;
    private int hydroCountPerPlayer;
    private boolean populateTextures;
    private boolean keepCurrentDecals;
    private boolean keepLayer0;
    private boolean smallWaterTexturesOnLayer5;
    private int moveLayer0ToAndSmooth;
    private int mapImageSize;
    private boolean restrictTextures;
    private boolean texturesInside;
    private int x1;
    private int x2;
    private int z1;
    private int z2;
    private boolean keepLayer1;
    private boolean keepLayer2;
    private boolean keepLayer3;
    private boolean keepLayer4;
    private boolean keepLayer5;
    private boolean keepLayer6;
    private boolean keepLayer7;
    private boolean keepLayer8;

    private BinaryMask resourceMask;
    private BinaryMask waterResourceMask;
    private BinaryMask plateauResourceMask;

    private SymmetrySettings symmetrySettings;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);
        if (DEBUG) {
            Path debugDir = Paths.get(".", "debug");
            FileUtils.deleteRecursiveIfExists(debugDir);
            Files.createDirectory(debugDir);
        }

        MapPopulator populator = new MapPopulator();

        populator.interpretArguments(args);

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
            System.out.println("map-transformer usage:\n" +
                    "--help                 produce help message\n" +
                    "--in-folder-path arg   required, set the input folder for the map\n" +
                    "--out-folder-path arg  required, set the output folder for the transformed map\n" +
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
                    "--keep-layer-0 arg     optional, populate where texture layer 0 is currently visible to replace layer number arg (1, 2, 3, 4, 5, 6, 7, 8)\n" +
                    " - to smooth this layer, add a 0 to its layer number arg: (10, 20, 30, 40, 50, 60, 70, 80)\n" +
                    "--texture-res arg      optional, set arg texture resolution (128, 256, 512, 1024, 2048, etc) - resolution cannot exceed map size (256 = 5 km)\n" +
                    "--textures-inside      optional, if x1/x2/z1/z2 are entered, textures will only be populated within the box formed between points those points \n" +
                    " - if this is not entered and if x1/x2/z1/z2 are entered, textures will only be populated outside of the box formed between points those points\n" +
                    "--x1 arg               optional, x-coordinate for point 1 for optional restriction on where textures will be populated\n" +
                    "--z1 arg               optional, z-coordinate for point 1 for optional restriction on where textures will be populated\n" +
                    "--x2 arg               optional, x-coordinate for point 2 for optional restriction on where textures will be populated\n" +
                    "--z2 arg               optional, z-coordinate for point 2 for optional restriction on where textures will be populated\n" +
                    "--lakes                optional, switches texturing for small bodies of water to layer 5 (instead of layer 6)\n" +
                    "--decals               optional, populate decals\n" +
                    "--ai                   optional, populate ai markers\n" +
                    "--keep-current-decals  optional, prevents decals currently on the map from being deleted\n" +
                    "--debug                optional, turn on debugging options\n");
            System.exit(0);
        }

        if (arguments.containsKey("debug")) {
            DEBUG = true;
        }

        if (!arguments.containsKey("in-folder-path")) {
            System.out.println("Input Folder not Specified");
            System.exit(1);
        }

        if (!arguments.containsKey("out-folder-path")) {
            System.out.println("Output Folder not Specified");
            System.exit(2);
        }

        if (!arguments.containsKey("team-symmetry") || !arguments.containsKey("spawn-symmetry")) {
            System.out.println("Symmetries not Specified");
            System.exit(3);
        }

        inMapPath = Paths.get(arguments.get("in-folder-path"));
        outFolderPath = Paths.get(arguments.get("out-folder-path"));
        symmetrySettings = new SymmetrySettings(Symmetry.valueOf(arguments.get("spawn-symmetry")), Symmetry.valueOf(arguments.get("team-symmetry")));
        symmetrySettings.setSpawnSymmetry(Symmetry.valueOf(arguments.get("spawn-symmetry")));
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
        keepLayer0 = arguments.containsKey("keep-layer-0");
        if (keepLayer0) {
            moveLayer0ToAndSmooth = Integer.parseInt(arguments.get("keep-layer-0"));
        }
        if (arguments.containsKey("texture-res")) {
            mapImageSize = Integer.parseInt(arguments.get("texture-res")) / 128 * 128;
        }
        texturesInside = arguments.containsKey("textures-inside");
        if (arguments.containsKey("x1") && arguments.containsKey("x2") && arguments.containsKey("z1") && arguments.containsKey("z2")) {
            x1 = Integer.parseInt(arguments.get("x1"));
            x2 = Integer.parseInt(arguments.get("x2"));
            z1 = Integer.parseInt(arguments.get("z1"));
            z2 = Integer.parseInt(arguments.get("z2"));
            restrictTextures = true;
        }
        smallWaterTexturesOnLayer5 = arguments.containsKey("lakes");
        populateDecals = arguments.containsKey("decals");
        populateAI = arguments.containsKey("ai");
        keepCurrentDecals = arguments.containsKey("keep-current-decals");
    }

    public void importMap() {
        try {
            File dir = inMapPath.toFile();

            File[] mapFiles = dir.listFiles((dir1, filename) -> filename.endsWith(".scmap"));
            if (mapFiles == null || mapFiles.length == 0) {
                System.out.println("No scmap file in map folder");
                return;
            }
            File scmapFile = mapFiles[0];
            mapFolder = inMapPath.getFileName().toString();
            mapName = scmapFile.getName().replace(".scmap", "");
            map = SCMapImporter.loadSCMAP(inMapPath);
            SaveImporter.importSave(inMapPath, map);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public void exportMap() {
        try {
            long startTime = System.currentTimeMillis();
            FileUtils.copyRecursiveIfExists(inMapPath, outFolderPath);
            SCMapExporter.exportSCMAP(outFolderPath.resolve(mapFolder), mapName, map);
            SaveExporter.exportSave(outFolderPath.resolve(mapFolder), mapName, map);
            ScenarioExporter.exportScenario(outFolderPath.resolve(mapFolder), mapName, map);
            Files.copy(inMapPath.resolve(mapName + "_script.lua"), outFolderPath.resolve(mapFolder).resolve(mapName + "_script.lua"), StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public void populate() {
        Random random = new Random();
        boolean waterPresent = map.getBiome().getWaterSettings().isWaterPresent();
        FloatMask heightmapBase = map.getHeightMask(symmetrySettings);
        heightmapBase.applySymmetry();
        map.setHeightImage(heightmapBase);
        float waterHeight;
        if (waterPresent) {
            waterHeight = map.getBiome().getWaterSettings().getElevation();
        } else {
            waterHeight = heightmapBase.getMin();
        }

        BinaryMask wholeMap = new BinaryMask(heightmapBase, 0f, random.nextLong());
        BinaryMask land = new BinaryMask(heightmapBase, waterHeight, random.nextLong());
        BinaryMask plateaus = new BinaryMask(heightmapBase, waterHeight + 3f, random.nextLong());
        FloatMask slope = new FloatMask(heightmapBase, random.nextLong()).gradient();
        BinaryMask impassable = new BinaryMask(slope, .9f, random.nextLong());
        BinaryMask ramps = new BinaryMask(slope, .25f, random.nextLong()).minus(impassable);
        BinaryMask passable = impassable.copy().invert();
        BinaryMask passableLand = new BinaryMask(land, null);
        BinaryMask passableWater = new BinaryMask(land, null).invert();

        if (populateSpawns) {
            if (spawnCount > 0) {
                map.setSpawnCountInit(spawnCount);
                SpawnGenerator spawnGenerator = new SpawnGenerator(map, random.nextLong(), 48);
                float spawnSeparation = StrictMath.max(random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16, 24);
                BinaryMask spawns = land.copy();
                spawns.intersect(passable).minus(ramps).deflate(16);
                spawnGenerator.generateSpawns(spawns, spawnSeparation);
                spawnGenerator.setMarkerHeights();
            } else {
                map.getSpawns().clear();
            }
        }

        if (populateMexes || populateHydros) {
            resourceMask = new BinaryMask(land, random.nextLong());
            waterResourceMask = new BinaryMask(land, random.nextLong()).invert();
            plateauResourceMask = new BinaryMask(land, random.nextLong());

            resourceMask.minus(impassable).deflate(8).minus(ramps);
            resourceMask.trimEdge(16).fillCenter(16, false);
            waterResourceMask.minus(ramps).deflate(16).trimEdge(16).fillCenter(16, false);
            plateauResourceMask.combine(resourceMask).intersect(plateaus).trimEdge(16).fillCenter(16, true);
        }

        if (populateMexes) {
            if (mexCountPerPlayer > 0) {
                map.setMexCountInit(mexCountPerPlayer * map.getSpawnCount());
                MexGenerator mexGenerator = new MexGenerator(map, random.nextLong(), 48, map.getSize() / 8);

                mexGenerator.generateMexes(resourceMask, plateauResourceMask, waterResourceMask);
                mexGenerator.setMarkerHeights();
            } else {
                map.getMexes().clear();
            }
        }

        if (populateHydros) {
            if (hydroCountPerPlayer > 0) {
                map.setMexCountInit(hydroCountPerPlayer * map.getSpawnCount());
                HydroGenerator hydroGenerator = new HydroGenerator(map, random.nextLong(), 48);

                hydroGenerator.generateHydros(resourceMask.deflate(4));
                hydroGenerator.setMarkerHeights();
            } else {
                map.getHydros().clear();
            }
        }

        if (populateTextures) {

            int smallWaterSizeLimit = 9000;

            FloatMask[] texturesMasks = map.getTextureMasksScaled(symmetrySettings);
            FloatMask oldLayer1 = texturesMasks[0];
            FloatMask oldLayer2 = texturesMasks[1];
            FloatMask oldLayer3 = texturesMasks[2];
            FloatMask oldLayer4 = texturesMasks[3];
            FloatMask oldLayer5 = texturesMasks[4];
            FloatMask oldLayer6 = texturesMasks[5];
            FloatMask oldLayer7 = texturesMasks[6];
            FloatMask oldLayer8 = texturesMasks[7];

            if (mapImageSize == 0 || mapImageSize > map.getSize()) {
                mapImageSize = map.getSize();
            }

            map.setTextureMasksLow(new BufferedImage(mapImageSize, mapImageSize, BufferedImage.TYPE_INT_ARGB));
            map.setTextureMasksHigh(new BufferedImage(mapImageSize, mapImageSize, BufferedImage.TYPE_INT_ARGB));

            oldLayer1.clampMin(0f).clampMax(1f).setSize(mapImageSize);
            oldLayer2.clampMin(0f).clampMax(1f).setSize(mapImageSize);
            oldLayer3.clampMin(0f).clampMax(1f).setSize(mapImageSize);
            oldLayer4.clampMin(0f).clampMax(1f).setSize(mapImageSize);
            oldLayer5.clampMin(0f).clampMax(1f).setSize(mapImageSize);
            oldLayer6.clampMin(0f).clampMax(1f).setSize(mapImageSize);
            oldLayer7.clampMin(0f).clampMax(1f).setSize(mapImageSize);
            oldLayer8.clampMin(0f).clampMax(1f).setSize(mapImageSize);

            BinaryMask water = new BinaryMask(land.copy().invert(), random.nextLong());
            BinaryMask flat = new BinaryMask(slope, .05f, random.nextLong()).invert();
            BinaryMask inland = new BinaryMask(land, random.nextLong());
            BinaryMask highGround = new BinaryMask(heightmapBase, waterHeight + 3f, random.nextLong());
            BinaryMask aboveBeach = new BinaryMask(heightmapBase, waterHeight + 1.5f, random.nextLong());
            BinaryMask aboveBeachEdge = new BinaryMask(heightmapBase, waterHeight + 3f, random.nextLong());
            BinaryMask flatAboveCoast = new BinaryMask(heightmapBase, waterHeight + 0.29f, random.nextLong());
            BinaryMask higherFlatAboveCoast = new BinaryMask(heightmapBase, waterHeight + 1.2f, random.nextLong());
            BinaryMask lowWaterBeach = new BinaryMask(heightmapBase, waterHeight, random.nextLong());
            BinaryMask tinyWater = water.copy().removeAreasBiggerThan(StrictMath.min(smallWaterSizeLimit / 4 + 750, smallWaterSizeLimit * 2 / 3));
            BinaryMask smallWater = water.copy().removeAreasBiggerThan(smallWaterSizeLimit);
            BinaryMask smallWaterBeach = smallWater.minus(tinyWater).inflate(2).combine(tinyWater);
            FloatMask smallWaterBeachTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);

            inland.deflate(2);
            flatAboveCoast.intersect(flat);
            higherFlatAboveCoast.intersect(flat);
            lowWaterBeach.invert().minus(smallWater).inflate(6).minus(aboveBeach);
            smallWaterBeach.minus(flatAboveCoast).smooth(2, 0.5f).minus(aboveBeach).minus(higherFlatAboveCoast).smooth(1);
            smallWaterBeachTexture.init(smallWaterBeach, 0, 1).smooth(8).clampMax(0.35f).add(smallWaterBeach, 1f).smooth(4).clampMax(0.65f).add(smallWaterBeach, 1f).smooth(1).add(smallWaterBeach, 1f).clampMax(1f);

            BinaryMask waterBeach = new BinaryMask(heightmapBase, waterHeight + 1f, random.nextLong());
            BinaryMask accentGround = new BinaryMask(land, random.nextLong());
            BinaryMask accentPlateau = new BinaryMask(plateaus, random.nextLong());
            BinaryMask slopes = new BinaryMask(slope, .1f, random.nextLong());
            BinaryMask accentSlopes = new BinaryMask(slope, .75f, random.nextLong()).invert();
            BinaryMask steepHills = new BinaryMask(slope, .55f, random.nextLong());
            BinaryMask rock = new BinaryMask(slope, 1.25f, random.nextLong());
            BinaryMask accentRock = new BinaryMask(slope, 1.25f, random.nextLong());
            FloatMask waterBeachTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentGroundTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentPlateauTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask slopesTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentSlopesTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask steepHillsTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask rockTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
            FloatMask accentRockTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);

            accentGround.minus(highGround).acid(.05f, 0).erode(.85f, symmetrySettings.getSpawnSymmetry()).smooth(2, .75f).acid(.45f, 0);
            accentPlateau.acid(.05f, 0).erode(.85f, symmetrySettings.getSpawnSymmetry()).smooth(2, .75f).acid(.45f, 0);
            slopes.intersect(land).flipValues(.95f).erode(.5f, symmetrySettings.getSpawnSymmetry()).acid(.3f, 0).erode(.2f, symmetrySettings.getSpawnSymmetry());
            accentSlopes.minus(flat).intersect(land).acid(.1f, 0).erode(.5f, symmetrySettings.getSpawnSymmetry()).smooth(4, .75f).acid(.55f, 0);
            steepHills.acid(.3f, 0).erode(.2f, symmetrySettings.getSpawnSymmetry());
            if (waterPresent) {
                waterBeach.invert().minus(smallWater).minus(flatAboveCoast).minus(inland).inflate(1).combine(lowWaterBeach).smooth(5, 0.5f).minus(aboveBeach).minus(higherFlatAboveCoast).smooth(2).smooth(1);
            } else {
                waterBeach.clear();
            }
            accentRock.acid(.2f, 0).erode(.3f, symmetrySettings.getSpawnSymmetry()).acid(.2f, 0).smooth(2, .5f).intersect(rock);

            accentGroundTexture.init(accentGround, 0, 1).smooth(8).add(accentGround, .65f).smooth(4).add(accentGround, .5f).smooth(1).clampMax(1f);
            accentPlateauTexture.init(accentPlateau, 0, 1).smooth(8).add(accentPlateau, .65f).smooth(4).add(accentPlateau, .5f).smooth(1).clampMax(1f);
            slopesTexture.init(slopes, 0, 1).smooth(8).add(slopes, .65f).smooth(4).add(slopes, .5f).smooth(1).clampMax(1f);
            accentSlopesTexture.init(accentSlopes, 0, 1).smooth(8).add(accentSlopes, .65f).smooth(4).add(accentSlopes, .5f).smooth(1).clampMax(1f);
            steepHillsTexture.init(steepHills, 0, 1).smooth(8).clampMax(0.35f).add(steepHills, .65f).smooth(4).clampMax(0.65f).add(steepHills, .5f).smooth(1).add(steepHills, 1f).clampMax(1f);
            waterBeachTexture.init(waterBeach, 0, 1).subtract(rock, 1f).subtract(aboveBeachEdge, 1f).clampMin(0).smooth(2, rock.copy().invert()).add(waterBeach, 1f).subtract(rock, 1f);
            waterBeachTexture.subtract(aboveBeachEdge, .9f).clampMin(0).smooth(2, rock.copy().invert()).subtract(rock, 1f).subtract(aboveBeachEdge, .8f).clampMin(0).add(waterBeach, .65f).smooth(2, rock.copy().invert());
            waterBeachTexture.subtract(rock, 1f).subtract(aboveBeachEdge, 0.7f).clampMin(0).add(waterBeach, .5f).smooth(2, rock.copy().invert()).smooth(2, rock.copy().invert()).subtract(rock, 1f).clampMin(0).smooth(2, rock.copy().invert());
            waterBeachTexture.smooth(2, rock.copy().invert()).subtract(rock, 1f).clampMin(0).smooth(2, rock.copy().invert()).smooth(1, rock.copy().invert()).smooth(1, rock.copy().invert()).clampMax(1f);
            waterBeachTexture.removeAreasOfSpecifiedSizeWithLocalMaximums(0, smallWaterSizeLimit, 15).smooth(1).smooth(1).reduceValuesOnIntersectingSmoothingZones(rock);
            if (smallWaterTexturesOnLayer5) {
                steepHillsTexture.add(smallWaterBeachTexture).clampMax(1f);
            } else {
                waterBeachTexture.add(smallWaterBeachTexture).clampMax(1f);
            }
            rockTexture.init(rock, 0, 1).smooth(8).clampMax(0.2f).add(rock, .65f).smooth(4).clampMax(0.3f).add(rock, .5f).smooth(1).add(rock, 1f).clampMax(1f);
            accentRockTexture.init(accentRock, 0, 1).clampMin(0).smooth(8).add(accentRock, .65f).smooth(4).add(accentRock, .5f).smooth(1).clampMax(1f);

            if (keepLayer1) {
                accentGroundTexture = new FloatMask(oldLayer1, null);
            }
            if (keepLayer2) {
                accentPlateauTexture = new FloatMask(oldLayer2, null);
            }
            if (keepLayer3) {
                slopesTexture = new FloatMask(oldLayer3, null);
            }
            if (keepLayer4) {
                accentSlopesTexture = new FloatMask(oldLayer4, null);
            }
            if (keepLayer5) {
                steepHillsTexture = new FloatMask(oldLayer5, null);
            }
            if (keepLayer6) {
                waterBeachTexture = new FloatMask(oldLayer6, null);
            }
            if (keepLayer7) {
                rockTexture = new FloatMask(oldLayer7, null);
            }
            if (keepLayer8) {
                accentRockTexture = new FloatMask(oldLayer8, null);
            }
            if (keepLayer0) {
                FloatMask oldLayer0 = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
                oldLayer0.init(wholeMap, 0f, 1f).subtract(oldLayer8).subtract(oldLayer7).subtract(oldLayer6).subtract(oldLayer5).subtract(oldLayer4).subtract(oldLayer3).subtract(oldLayer2).subtract(oldLayer1).clampMin(0f);
                FloatMask oldLayer0Texture = new FloatMask(oldLayer0, null);
                if (moveLayer0ToAndSmooth >= 10) {
                    oldLayer0Texture.smooth(8).clampMax(0.35f).add(oldLayer0).smooth(4).clampMax(0.65f).add(oldLayer0).smooth(1).add(oldLayer0).clampMax(1f);
                }
                switch (moveLayer0ToAndSmooth) {
                    case 1, 10 -> accentGroundTexture = new FloatMask(oldLayer0Texture, null);
                    case 2, 20 -> accentPlateauTexture = new FloatMask(oldLayer0Texture, null);
                    case 3, 30 -> slopesTexture = new FloatMask(oldLayer0Texture, null);
                    case 4, 40 -> accentSlopesTexture = new FloatMask(oldLayer0Texture, null);
                    case 5, 50 -> steepHillsTexture = new FloatMask(oldLayer0Texture, null);
                    case 6, 60 -> waterBeachTexture = new FloatMask(oldLayer0Texture, null);
                    case 7, 70 -> rockTexture = new FloatMask(oldLayer0Texture, null);
                    case 8, 80 -> accentRockTexture = new FloatMask(oldLayer0Texture, null);
                }
            }

            if (restrictTextures && x1 >= 0 && x1 <= mapImageSize && x2 >= 0 && x2 <= mapImageSize && z1 >= 0 && z1 <= mapImageSize && z2 >= 0 && z2 <= mapImageSize) {
                BinaryMask textureBox = new BinaryMask(mapImageSize, random.nextLong(), symmetrySettings);
                if (texturesInside) {
                    textureBox.invert().setRectangularAreaFromPoints(x1, x2, z1, z2, true);
                } else {
                    textureBox.setRectangularAreaFromPoints(x1, x2, z1, z2, false);
                }
                accentGroundTexture.replaceValuesInRangeWith(textureBox, oldLayer1);
                accentPlateauTexture.replaceValuesInRangeWith(textureBox, oldLayer2);
                slopesTexture.replaceValuesInRangeWith(textureBox, oldLayer3);
                accentSlopesTexture.replaceValuesInRangeWith(textureBox, oldLayer4);
                steepHillsTexture.replaceValuesInRangeWith(textureBox, oldLayer5);
                waterBeachTexture.replaceValuesInRangeWith(textureBox, oldLayer6);
                rockTexture.replaceValuesInRangeWith(textureBox, oldLayer7);
                accentRockTexture.replaceValuesInRangeWith(textureBox, oldLayer8);
            }

            map.setTextureMasksLowScaled(accentGroundTexture, accentPlateauTexture, slopesTexture, accentSlopesTexture);
            map.setTextureMasksHighScaled(steepHillsTexture, waterBeachTexture, rockTexture, accentRockTexture);
        }

        if (populateProps) {
            map.getProps().clear();
            PropGenerator propGenerator = new PropGenerator(map, random.nextLong());
            PropMaterials propMaterials = null;

            try {
                propMaterials = FileUtils.deserialize(propsPath.getParent(), propsPath.getFileName().toString(), PropMaterials.class);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.print("An error occured while loading props\n");
                System.exit(1);
            }
            BinaryMask treeMask = new BinaryMask(map.getSize() / 16, random.nextLong(), symmetrySettings);
            BinaryMask cliffRockMask = new BinaryMask(map.getSize() / 16, random.nextLong(), symmetrySettings);
            BinaryMask fieldStoneMask = new BinaryMask(map.getSize() / 4, random.nextLong(), symmetrySettings);
            BinaryMask largeRockFieldMask = new BinaryMask(map.getSize() / 4, random.nextLong(), symmetrySettings);
            BinaryMask smallRockFieldMask = new BinaryMask(map.getSize() / 4, random.nextLong(), symmetrySettings);

            cliffRockMask.randomize(.4f).intersect(impassable).grow(.5f, symmetrySettings.getSpawnSymmetry(), 4).minus(plateaus.copy().outline()).intersect(land);
            fieldStoneMask.randomize(random.nextFloat() * .001f).enlarge(256).intersect(land).minus(impassable);
            fieldStoneMask.enlarge(map.getSize() + 1).trimEdge(10);
            treeMask.randomize(.2f).enlarge(map.getSize() / 4).inflate(2).erode(.5f, symmetrySettings.getSpawnSymmetry()).smooth(4, .75f).erode(.5f, symmetrySettings.getSpawnSymmetry());
            treeMask.enlarge(map.getSize() + 1).intersect(land.copy().deflate(8)).minus(impassable.copy().inflate(2)).deflate(2).trimEdge(8).smooth(4, .25f);
            largeRockFieldMask.randomize(random.nextFloat() * .001f).trimEdge(map.getSize() / 16).grow(.5f, symmetrySettings.getSpawnSymmetry(), 3).intersect(land).minus(impassable);
            smallRockFieldMask.randomize(random.nextFloat() * .003f).trimEdge(map.getSize() / 64).grow(.5f, symmetrySettings.getSpawnSymmetry()).intersect(land).minus(impassable);

            BinaryMask noProps = new BinaryMask(impassable, null);

            for (int i = 0; i < map.getSpawnCount(); i++) {
                noProps.fillCircle(map.getSpawn(i), 30, true);
            }
            for (int i = 0; i < map.getMexCount(); i++) {
                noProps.fillCircle(map.getMex(i), 10, true);
            }
            for (int i = 0; i < map.getHydroCount(); i++) {
                noProps.fillCircle(map.getHydro(i), 16, true);
            }

            if (propMaterials.getTreeGroups() != null && propMaterials.getTreeGroups().length > 0) {
                propGenerator.generateProps(treeMask.minus(noProps), propMaterials.getTreeGroups(), 3f);
            }
            if (propMaterials.getRocks() != null && propMaterials.getRocks().length > 0) {
                propGenerator.generateProps(cliffRockMask.minus(noProps), propMaterials.getRocks(), 1.5f);
                propGenerator.generateProps(largeRockFieldMask.minus(noProps), propMaterials.getRocks(), 1.5f);
                propGenerator.generateProps(smallRockFieldMask.minus(noProps), propMaterials.getRocks(), 1.5f);
            }
            if (propMaterials.getBoulders() != null && propMaterials.getBoulders().length > 0) {
                propGenerator.generateProps(fieldStoneMask.minus(noProps), propMaterials.getBoulders(), 30f);
            }

            propGenerator.setPropHeights();
        }

        if (populateDecals) {
            if (!keepCurrentDecals) {
                map.getDecals().clear();
            }
            DecalGenerator decalGenerator = new DecalGenerator(map, random.nextLong());

            BinaryMask intDecal = new BinaryMask(land, random.nextLong());
            BinaryMask rockDecal = new BinaryMask(slope, 1.25f, random.nextLong());

            BinaryMask noDecals = new BinaryMask(map.getSize() + 1, null, symmetrySettings);

            for (int i = 0; i < map.getSpawnCount(); i++) {
                noDecals.fillCircle(map.getSpawn(i), 24, true);
            }

            decalGenerator.generateDecals(intDecal.minus(noDecals), DecalGenerator.INT, 96f, 64f);
            decalGenerator.generateDecals(rockDecal.minus(noDecals), DecalGenerator.ROCKS, 8f, 16f);
        }

        if (populateAI) {
            BinaryMask passableAI = passable.copy().deflate(6).trimEdge(8);
            passableLand.deflate(4).intersect(passableAI);
            passableWater.deflate(16).trimEdge(8);
            AIMarkerGenerator aiMarkerGenerator = new AIMarkerGenerator(map, 0);
            aiMarkerGenerator.generateAIMarkers(passableAI, passableLand, passableWater, 8, 16, false);
            aiMarkerGenerator.setMarkerHeights();
        }
    }
}
