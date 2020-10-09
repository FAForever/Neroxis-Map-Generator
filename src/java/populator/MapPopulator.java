package populator;

import export.SCMapExporter;
import export.SaveExporter;
import export.ScenarioExporter;
import export.ScriptExporter;
import generator.AIMarkerGenerator;
import generator.HydroGenerator;
import generator.MexGenerator;
import generator.SpawnGenerator;
import importer.SCMapImporter;
import importer.SaveImporter;
import map.*;
import util.ArgumentParser;
import util.FileUtils;

import java.io.File;
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
    private Random random;
    private boolean populateSpawns;
    private boolean populateMexes;
    private boolean populateHydros;
    private boolean populateProps;
    private boolean populateAI;
    private int spawnCount;
    private int mexCountPerPlayer;
    private int hydroCountPerPlayer;
    private boolean populateTextures;

    //masks used in transformation
    private BinaryMask land;
    private BinaryMask mountains;
    private BinaryMask plateaus;
    private BinaryMask ramps;
    private BinaryMask impassable;
    private BinaryMask passable;
    private BinaryMask passableLand;
    private BinaryMask passableWater;
    private FloatMask heightmapBase;
    private BinaryMask resourceMask;
    private BinaryMask waterResourceMask;
    private BinaryMask plateauResourceMask;

    private SymmetryHierarchy symmetryHierarchy;

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
        System.out.println("Terrain Symmetry: " + populator.symmetryHierarchy.getTerrainSymmetry());
        System.out.println("Team Symmetry: " + populator.symmetryHierarchy.getTeamSymmetry());
        System.out.println("Spawn Symmetry: " + populator.symmetryHierarchy.getSpawnSymmetry());
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
                    "--team-symmetry arg    required, set the symmetry for the teams(X, Y, XY, YX)\n" +
                    "--spawn-symmetry arg   required, set the symmetry for the spawns(POINT, X, Y, XY, YX)\n" +
                    "--spawns arg           optional, populate arg spawns\n" +
                    "--mexes arg            optional, populate arg mexes per player\n" +
                    "--hydros arg           optional, populate arg hydros per player\n" +
                    "--props                optional, populate props\n" +
                    "--wrecks               optional, populate wrecks\n" +
                    "--textures             optional, populate texture masks\n" +
                    "--ai                   optional, populate ai markers\n" +
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
        symmetryHierarchy = new SymmetryHierarchy(Symmetry.valueOf(arguments.get("spawn-symmetry")), Symmetry.valueOf(arguments.get("team-symmetry")));
        symmetryHierarchy.setSpawnSymmetry(Symmetry.valueOf(arguments.get("spawn-symmetry")));
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
        populateTextures = arguments.containsKey("textures");
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
            ScriptExporter.exportScript(outFolderPath.resolve(mapFolder), mapName, map);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public void populate() {
        random = new Random();
        boolean waterPresent = map.getBiome().getWaterSettings().isWaterPresent();
        heightmapBase = map.getHeightMask(symmetryHierarchy);
        heightmapBase.applySymmetry();
        map.setHeightmap(heightmapBase);
        float waterHeight;
        if (waterPresent) {
            waterHeight = map.getBiome().getWaterSettings().getElevation();
        } else {
            waterHeight = heightmapBase.getMin();
        }
        land = new BinaryMask(heightmapBase, waterHeight, random.nextLong());
        plateaus = new BinaryMask(heightmapBase, waterHeight + 3f, random.nextLong());
        FloatMask slope = new FloatMask(heightmapBase, random.nextLong()).gradient();
        slope.startVisualDebugger("s");
        impassable = new BinaryMask(slope, .9f, random.nextLong());
        ramps = new BinaryMask(slope, .25f, random.nextLong()).minus(impassable);
        passable = impassable.copy().invert();
        passableLand = new BinaryMask(land, null);
        passableWater = new BinaryMask(land, null).invert();

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
            map.getDecals().clear();

            BinaryMask flat = new BinaryMask(slope, .05f, random.nextLong()).invert();
            BinaryMask ground = new BinaryMask(land, random.nextLong());
            BinaryMask accentGround = new BinaryMask(land, random.nextLong());
            BinaryMask accentPlateau = new BinaryMask(plateaus, random.nextLong());
            BinaryMask slopes = new BinaryMask(slope, .1f, random.nextLong());
            BinaryMask accentSlopes = new BinaryMask(slope, .75f, random.nextLong()).invert();
            BinaryMask rockBase = new BinaryMask(slope, .75f, random.nextLong());
            BinaryMask rock = new BinaryMask(slope, 1.25f, random.nextLong());
            BinaryMask accentRock = new BinaryMask(rock, random.nextLong());
            FloatMask groundTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask accentGroundTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask accentPlateauTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask slopesTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask accentSlopesTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask rockBaseTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask rockTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask accentRockTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);

            ground.shrink(map.getSize() / 4).erode(.75f, symmetryHierarchy.getSpawnSymmetry(), 8).grow(.2f, symmetryHierarchy.getSpawnSymmetry(), 8);
            ground.combine(plateaus).intersect(land).smooth(2, .25f).filterShapes(32);
            accentGround.minus(slopes).minus(plateaus).deflate(4).acid(.1f, 0).erode(.5f, symmetryHierarchy.getSpawnSymmetry()).smooth(16, .75f);
            accentPlateau.minus(slopes).acid(.1f, 0).erode(.5f, symmetryHierarchy.getSpawnSymmetry()).smooth(16, .75f);
            slopes.intersect(land).flipValues(.75f).erode(.5f, symmetryHierarchy.getSpawnSymmetry());
            accentSlopes.minus(flat).intersect(land).acid(.1f, 0).erode(.5f, symmetryHierarchy.getSpawnSymmetry()).smooth(8, .75f);
            rockBase.intersect(land);
            accentRock.acid(.1f, 0).erode(.5f, symmetryHierarchy.getSpawnSymmetry()).smooth(8, .25f).intersect(rock);

            groundTexture.init(ground, 0, 1).smooth(4);
            accentGroundTexture.init(accentGround, 0, 1).smooth(8);
            accentPlateauTexture.init(accentPlateau, 0, 1).smooth(8);
            slopesTexture.init(slopes, 0, 1).smooth(4);
            accentSlopesTexture.init(accentSlopes, 0, 1).smooth(8);
            rockBaseTexture.init(rockBase, 0, 1).smooth(2);
            rockTexture.init(rock, 0, 1).smooth(2).add(rock.copy().shrink(map.getSize() / 2), .75f).smooth(1).add(rock.copy().shrink(map.getSize() / 2), .5f);
            accentRockTexture.init(accentRock, 0, 1).smooth(2);

            map.setTextureMasksLow(groundTexture, accentGroundTexture, accentPlateauTexture, slopesTexture);
            map.setTextureMasksHigh(accentSlopesTexture, rockBaseTexture, rockTexture, accentRockTexture);
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
