package neroxis.populator;

import neroxis.exporter.MapExporter;
import neroxis.generator.*;
import neroxis.importer.MapImporter;
import neroxis.map.*;
import neroxis.util.ArgumentParser;
import neroxis.util.FileUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private int spawnCount;
    private int mexCountPerPlayer;
    private int hydroCountPerPlayer;
    private boolean populateTextures;
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

    private SymmetrySettings symmetrySettings;

    public static void main(String[] args) throws Exception {

        Locale.setDefault(Locale.US);
        if (DEBUG) {
            Path debugDir = Paths.get(".", "debug");
            FileUtils.deleteRecursiveIfExists(debugDir);
            Files.createDirectory(debugDir);
        }

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
            return;
        }

        if (arguments.containsKey("debug")) {
            DEBUG = true;
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
        try {
            long startTime = System.currentTimeMillis();
            MapExporter.exportMap(outFolderPath, map, true);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
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
        FloatMask heightmapBase = map.getHeightMask(symmetrySettings);
        heightmapBase = new FloatMask(heightmapBase, random.nextLong());
        heightmapBase.applySymmetry(SymmetryType.SPAWN);
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
        BinaryMask passableLand = new BinaryMask(land, random.nextLong());
        BinaryMask passableWater = new BinaryMask(land, random.nextLong()).invert();

        if (populateSpawns) {
            if (spawnCount > 0) {
                map.setSpawnCountInit(spawnCount);
                SpawnPlacer spawnPlacer = new SpawnPlacer(map, random.nextLong());
                float spawnSeparation = StrictMath.max(random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16, 24);
                BinaryMask spawns = land.copy();
                spawns.intersect(passable).minus(ramps).deflate(16);
                spawnPlacer.placeSpawns(spawns, spawnSeparation);
            } else {
                map.getSpawns().clear();
            }
        }

        if (populateMexes || populateHydros) {
            resourceMask = new BinaryMask(land, random.nextLong());
            waterResourceMask = new BinaryMask(land, random.nextLong()).invert();

            resourceMask.minus(impassable).deflate(8).minus(ramps);
            resourceMask.fillEdge(16, false).fillCenter(16, false);
            waterResourceMask.minus(ramps).deflate(16).fillEdge(16, false).fillCenter(16, false);
        }

        if (populateMexes) {
            if (mexCountPerPlayer > 0) {
                map.setMexCountInit(mexCountPerPlayer * map.getSpawnCount());
                MexPlacer mexPlacer = new MexPlacer(map, random.nextLong());

                mexPlacer.placeMexes(resourceMask, waterResourceMask);
            } else {
                map.getMexes().clear();
            }
        }

        if (populateHydros) {
            if (hydroCountPerPlayer > 0) {
                map.setMexCountInit(hydroCountPerPlayer * map.getSpawnCount());
                HydroPlacer hydroPlacer = new HydroPlacer(map, random.nextLong());

                hydroPlacer.placeHydros(resourceMask.deflate(4));
            } else {
                map.getHydros().clear();
            }
        }

        if (populateTextures) {

            int smallWaterSizeLimit = 9000;

            FloatMask[] texturesMasks = map.getTextureMasksScaled(symmetrySettings);

            if (mapImageSize == 0 || mapImageSize > map.getSize()) {
                mapImageSize = map.getSize();
            }

            map.setTextureMasksLow(new BufferedImage(mapImageSize, mapImageSize, BufferedImage.TYPE_INT_ARGB));
            map.setTextureMasksHigh(new BufferedImage(mapImageSize, mapImageSize, BufferedImage.TYPE_INT_ARGB));

            texturesMasks[0].clampMin(0f).clampMax(1f).resample(mapImageSize);
            texturesMasks[1].clampMin(0f).clampMax(1f).resample(mapImageSize);
            texturesMasks[2].clampMin(0f).clampMax(1f).resample(mapImageSize);
            texturesMasks[3].clampMin(0f).clampMax(1f).resample(mapImageSize);
            texturesMasks[4].clampMin(0f).clampMax(1f).resample(mapImageSize);
            texturesMasks[5].clampMin(0f).clampMax(1f).resample(mapImageSize);
            texturesMasks[6].clampMin(0f).clampMax(1f).resample(mapImageSize);
            texturesMasks[7].clampMin(0f).clampMax(1f).resample(mapImageSize);

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
            BinaryMask smallWaterBeach = smallWater.copy().minus(tinyWater).inflate(2).combine(tinyWater);
            FloatMask smallWaterBeachTexture = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);

            inland.deflate(2);
            flatAboveCoast.intersect(flat);
            higherFlatAboveCoast.intersect(flat);
            lowWaterBeach.invert().minus(smallWater).inflate(6).minus(aboveBeach);
            smallWaterBeach.minus(flatAboveCoast).smooth(2, 0.5f).minus(aboveBeach).minus(higherFlatAboveCoast).smooth(1);
            smallWaterBeach.setSize(mapImageSize);

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

            accentGround.minus(highGround).acid(.05f, 0).erode(.85f, SymmetryType.SPAWN).smooth(2, .75f).acid(.45f, 0);
            accentPlateau.acid(.05f, 0).erode(.85f, SymmetryType.SPAWN).smooth(2, .75f).acid(.45f, 0);
            slopes.intersect(land).flipValues(.95f).erode(.5f, SymmetryType.SPAWN).acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
            accentSlopes.minus(flat).intersect(land).acid(.1f, 0).erode(.5f, SymmetryType.SPAWN).smooth(4, .75f).acid(.55f, 0);
            steepHills.acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
            if (waterPresent) {
                waterBeach.invert().minus(smallWater).minus(flatAboveCoast).minus(inland).inflate(1).combine(lowWaterBeach).smooth(5, 0.5f).minus(aboveBeach).minus(higherFlatAboveCoast).smooth(2).smooth(1);
            } else {
                waterBeach.clear();
            }
            accentRock.acid(.2f, 0).erode(.3f, SymmetryType.SPAWN).acid(.2f, 0).smooth(2, .5f).intersect(rock);

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
            accentGroundTexture.init(accentGround, 0, 1).smooth(8).add(accentGround, .65f).smooth(4).add(accentGround, .5f).smooth(1).clampMax(1f);
            accentPlateauTexture.init(accentPlateau, 0, 1).smooth(8).add(accentPlateau, .65f).smooth(4).add(accentPlateau, .5f).smooth(1).clampMax(1f);
            slopesTexture.init(slopes, 0, 1).smooth(8).add(slopes, .65f).smooth(4).add(slopes, .5f).smooth(1).clampMax(1f);
            accentSlopesTexture.init(accentSlopes, 0, 1).smooth(8).add(accentSlopes, .65f).smooth(4).add(accentSlopes, .5f).smooth(1).clampMax(1f);
            steepHillsTexture.init(steepHills, 0, 1).smooth(8).clampMax(0.35f).add(steepHills, .65f).smooth(4).clampMax(0.65f).add(steepHills, .5f).smooth(1).add(steepHills, 1f).clampMax(1f);
            waterBeachTexture.init(waterBeach, 0, 1).subtract(rock, 1f).subtract(aboveBeachEdge, 1f).clampMin(0).smooth(2, rock.copy().invert()).add(waterBeach, 1f).subtract(rock, 1f);
            waterBeachTexture.subtract(aboveBeachEdge, .9f).clampMin(0).smooth(2, rock.copy().invert()).subtract(rock, 1f).subtract(aboveBeachEdge, .8f).clampMin(0).add(waterBeach, .65f).smooth(2, rock.copy().invert());
            waterBeachTexture.subtract(rock, 1f).subtract(aboveBeachEdge, 0.7f).clampMin(0).add(waterBeach, .5f).smooth(2, rock.copy().invert()).smooth(2, rock.copy().invert()).subtract(rock, 1f).clampMin(0).smooth(2, rock.copy().invert());
            waterBeachTexture.smooth(2, rock.copy().invert()).subtract(rock, 1f).clampMin(0).smooth(2, rock.copy().invert()).smooth(1, rock.copy().invert()).smooth(1, rock.copy().invert()).clampMax(1f);
            waterBeachTexture.removeAreasOfSpecifiedSizeWithLocalMaximums(0, smallWaterSizeLimit, 15, 1f).smooth(1).smooth(1);
            if (smallWaterTexturesOnLayer5) {
                steepHillsTexture.add(smallWaterBeachTexture).clampMax(1f);
            } else {
                waterBeachTexture.add(smallWaterBeachTexture).clampMax(1f);
            }
            rockTexture.init(rock, 0, 1).smooth(8).clampMax(0.2f).add(rock, .65f).smooth(4).clampMax(0.3f).add(rock, .5f).smooth(1).add(rock, 1f).clampMax(1f);
            accentRockTexture.init(accentRock, 0, 1).clampMin(0).smooth(8).add(accentRock, .65f).smooth(4).add(accentRock, .5f).smooth(1).clampMax(1f);

            if (keepLayer1) {
                accentGroundTexture = new FloatMask(texturesMasks[0], null);
            }
            if (keepLayer2) {
                accentPlateauTexture = new FloatMask(texturesMasks[1], null);
            }
            if (keepLayer3) {
                slopesTexture = new FloatMask(texturesMasks[2], null);
            }
            if (keepLayer4) {
                accentSlopesTexture = new FloatMask(texturesMasks[3], null);
            }
            if (keepLayer5) {
                steepHillsTexture = new FloatMask(texturesMasks[4], null);
            }
            if (keepLayer6) {
                waterBeachTexture = new FloatMask(texturesMasks[5], null);
            }
            if (keepLayer7) {
                rockTexture = new FloatMask(texturesMasks[6], null);
            }
            if (keepLayer8) {
                accentRockTexture = new FloatMask(texturesMasks[7], null);
            }
            if (keepLayer0) {
                FloatMask oldLayer0 = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings);
                oldLayer0.init(wholeMap, 0f, 1f).subtract(texturesMasks[7]).subtract(texturesMasks[6]).subtract(texturesMasks[5]).subtract(texturesMasks[4]).subtract(texturesMasks[3]).subtract(texturesMasks[2]).subtract(texturesMasks[1]).subtract(texturesMasks[0]).clampMin(0f);
                FloatMask oldLayer0Texture = new FloatMask(oldLayer0, null);
                if (moveLayer0ToAndSmooth >= 10) {
                    oldLayer0Texture.smooth(8).clampMax(0.35f).add(oldLayer0).smooth(4).clampMax(0.65f).add(oldLayer0).smooth(1).add(oldLayer0).clampMax(1f);
                }
                switch (moveLayer0ToAndSmooth) {
                    case 1:
                    case 10:
                        accentGroundTexture = new FloatMask(oldLayer0Texture, null);
                        break;
                    case 2:
                    case 20:
                        accentPlateauTexture = new FloatMask(oldLayer0Texture, null);
                        break;
                    case 3:
                    case 30:
                        slopesTexture = new FloatMask(oldLayer0Texture, null);
                        break;
                    case 4:
                    case 40:
                        accentSlopesTexture = new FloatMask(oldLayer0Texture, null);
                        break;
                    case 5:
                    case 50:
                        steepHillsTexture = new FloatMask(oldLayer0Texture, null);
                        break;
                    case 6:
                    case 60:
                        waterBeachTexture = new FloatMask(oldLayer0Texture, null);
                        break;
                    case 7:
                    case 70:
                        rockTexture = new FloatMask(oldLayer0Texture, null);
                        break;
                    case 8:
                    case 80:
                        accentRockTexture = new FloatMask(oldLayer0Texture, null);
                        break;
                }
            }

            if (restrictTextures && x1 >= 0 && x1 <= mapImageSize && x2 >= 0 && x2 <= mapImageSize && z1 >= 0 && z1 <= mapImageSize && z2 >= 0 && z2 <= mapImageSize) {
                BinaryMask textureBox = new BinaryMask(mapImageSize, random.nextLong(), symmetrySettings);
                if (texturesInside) {
                    textureBox.invert().fillRectFromPoints(x1, x2, z1, z2, true);
                } else {
                    textureBox.fillRectFromPoints(x1, x2, z1, z2, false);
                }
                accentGroundTexture.replaceValues(textureBox, texturesMasks[0]);
                accentPlateauTexture.replaceValues(textureBox, texturesMasks[1]);
                slopesTexture.replaceValues(textureBox, texturesMasks[2]);
                accentSlopesTexture.replaceValues(textureBox, texturesMasks[3]);
                steepHillsTexture.replaceValues(textureBox, texturesMasks[4]);
                waterBeachTexture.replaceValues(textureBox, texturesMasks[5]);
                rockTexture.replaceValues(textureBox, texturesMasks[6]);
                accentRockTexture.replaceValues(textureBox, texturesMasks[7]);
            }

            map.setTextureMasksLowScaled(accentGroundTexture, accentPlateauTexture, slopesTexture, accentSlopesTexture);
            map.setTextureMasksHighScaled(steepHillsTexture, waterBeachTexture, rockTexture, accentRockTexture);
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

            BinaryMask flatEnough = new BinaryMask(slope, .02f, random.nextLong());
            BinaryMask flatish = new BinaryMask(slope, .042f, random.nextLong());
            BinaryMask nearSteepHills = new BinaryMask(slope,.55f, random.nextLong());
            BinaryMask flatEnoughNearRock = new BinaryMask(slope,1.25f, random.nextLong());
            flatEnough.invert().intersect(land).erode(.15f);
            flatish.invert().intersect(land).erode(.15f);
            nearSteepHills.inflate(6).combine(flatEnoughNearRock.copy().inflate(16).intersect(nearSteepHills.copy().inflate(11).minus(flatEnough))).intersect(land.copy().deflate(10));
            flatEnoughNearRock.inflate(7).combine(nearSteepHills).intersect(flatish);

            BinaryMask treeMask = new BinaryMask(flatEnough, random.nextLong());
            BinaryMask cliffRockMask = new BinaryMask(land, random.nextLong());
            BinaryMask fieldStoneMask = new BinaryMask(land, random.nextLong());
            BinaryMask largeRockFieldMask = new BinaryMask(land, random.nextLong());
            BinaryMask smallRockFieldMask = new BinaryMask(land, random.nextLong());

            treeMask.deflate(6).erode(0.5f).intersect(land.copy().deflate(15).acid(.05f, 0).erode(.85f, SymmetryType.SPAWN).smooth(2, .75f).acid(.45f, 0));
            cliffRockMask.randomize(.017f).intersect(flatEnoughNearRock);
            fieldStoneMask.randomize(.00145f).intersect(flatEnoughNearRock.copy().deflate(1));
            largeRockFieldMask.randomize(.015f).intersect(flatEnoughNearRock);
            smallRockFieldMask.randomize(.015f).intersect(flatEnoughNearRock);

            BinaryMask noProps = new BinaryMask(impassable, null);

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
                propPlacer.placeProps(treeMask.minus(noProps), propMaterials.getTreeGroups(), 3f);
            }
            if (propMaterials.getRocks() != null && propMaterials.getRocks().length > 0) {
                propPlacer.placeProps(cliffRockMask.minus(noProps), propMaterials.getRocks(), 1.5f);
                propPlacer.placeProps(largeRockFieldMask.minus(noProps), propMaterials.getRocks(), 1.5f);
                propPlacer.placeProps(smallRockFieldMask.minus(noProps), propMaterials.getRocks(), 1.5f);
            }
            if (propMaterials.getBoulders() != null && propMaterials.getBoulders().length > 0) {
                propPlacer.placeProps(fieldStoneMask.minus(noProps), propMaterials.getBoulders(), 30f);
            }
        }

        if (populateAI) {
            map.getAmphibiousAIMarkers().clear();
            map.getLandAIMarkers().clear();
            map.getNavyAIMarkers().clear();
            map.getAirAIMarkers().clear();
            BinaryMask passableAI = passable.copy().combine(land.copy().invert()).fillEdge(8, false);
            passableLand.intersect(passableAI);
            passableWater.deflate(16).intersect(passableAI).fillEdge(8, false);
            AIMarkerPlacer.placeAIMarkers(passableAI, map.getAmphibiousAIMarkers(), "AmphPN%d");
            AIMarkerPlacer.placeAIMarkers(passableLand, map.getLandAIMarkers(), "LandPN%d");
            AIMarkerPlacer.placeAIMarkers(passableWater, map.getNavyAIMarkers(), "NavyPN%d");
            AIMarkerPlacer.placeAirAIMarkers(map);
        }

        map.setHeights();
    }
}
