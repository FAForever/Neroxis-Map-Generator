package populator;

import biomes.Biome;
import biomes.Biomes;
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
import util.serialized.WaterSettings;

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
    private Biome biome;

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
                    "--team-symmetry arg    optional, set the symmetry for the teams(X, Y, XY, YX)\n" +
                    "--spawn-symmetry arg   optional, set the symmetry for the spawns(Point, X, Y, XY, YX)\n" +
                    "--spawns arg           optional, populate spawns" +
                    "--mexes arg            optional, populate mexes" +
                    "--hydros arg           optional, populate hydros" +
                    "--props                optional, populate props" +
                    "--wrecks               optional, populate wrecks" +
                    "--textures biome       optional, populate textures with biome" +
                    "--ai                   optional, populate ai markers" +
                    "--debug                optional, turn on debugging options");
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
        if (populateTextures) {
            biome = Biomes.getBiomeByName(arguments.get("textures"));
        }
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
            FileUtils.deleteRecursiveIfExists(outFolderPath.resolve(mapName));

            long startTime = System.currentTimeMillis();
            Files.createDirectories(outFolderPath.resolve(mapName));
            SCMapExporter.exportSCMAP(outFolderPath, mapName, map);
            SaveExporter.exportSave(outFolderPath, mapName, map);
            ScenarioExporter.exportScenario(outFolderPath, mapName, map);
            ScriptExporter.exportScript(outFolderPath, mapName, map);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public void populate() {
        random = new Random();
        boolean waterPresent = map.getBiome().getWaterSettings().isWaterPresent();
        float waterHeight;
        if (waterPresent) {
            waterHeight = map.getBiome().getWaterSettings().getElevation();
        } else {
            waterHeight = 0;
        }
        heightmapBase = map.getHeightMask(symmetryHierarchy);
        heightmapBase.applySymmetry();
        map.setHeightmap(heightmapBase);
        land = new BinaryMask(heightmapBase, waterHeight, random.nextLong());
        plateaus = new BinaryMask(heightmapBase, waterHeight + 3f, random.nextLong());
        FloatMask slope = new FloatMask(heightmapBase, random.nextLong()).gradient();
        impassable = new BinaryMask(slope, 1f, random.nextLong());
        ramps = new BinaryMask(slope, .25f, random.nextLong()).minus(impassable);
        passable = impassable.copy().invert();
        passableLand = new BinaryMask(land, null);
        passableWater = new BinaryMask(land, null).invert();

        if (populateSpawns) {
            map.setSpawnCountInit(spawnCount);
            SpawnGenerator spawnGenerator = new SpawnGenerator(map, random.nextLong(), 48);
            float spawnSeparation = StrictMath.max(random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16, 24);
            BinaryMask spawns = land.copy();
            spawns.intersect(passable).minus(ramps).deflate(16);
            spawnGenerator.generateSpawns(spawns, spawnSeparation);
            spawnGenerator.setMarkerHeights();
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
            map.setMexCountInit(mexCountPerPlayer * map.getSpawnCount());
            MexGenerator mexGenerator = new MexGenerator(map, random.nextLong(), 48, map.getSize() / 8);

            mexGenerator.generateMexes(resourceMask, plateauResourceMask, waterResourceMask);
            mexGenerator.setMarkerHeights();
        }

        if (populateHydros) {
            map.setMexCountInit(hydroCountPerPlayer * map.getSpawnCount());
            HydroGenerator hydroGenerator = new HydroGenerator(map, random.nextLong(), 48);

            hydroGenerator.generateHydros(resourceMask.deflate(4));
            hydroGenerator.setMarkerHeights();
        }

        if (populateTextures) {
            WaterSettings mapWaterSettings = map.getBiome().getWaterSettings();
            WaterSettings biomeWaterSettings = biome.getWaterSettings();
            biomeWaterSettings.setWaterPresent(mapWaterSettings.isWaterPresent());
            biomeWaterSettings.setElevation(mapWaterSettings.getElevation());
            biomeWaterSettings.setElevationAbyss(mapWaterSettings.getElevationAbyss());
            biomeWaterSettings.setElevationDeep(mapWaterSettings.getElevationDeep());
            map.getDecals().clear();
            map.setBiome(biome);

            BinaryMask ground = new BinaryMask(land, random.nextLong());
//            BinaryMask accentGround = new BinaryMask(hills, random.nextLong());
            BinaryMask highlightGround = new BinaryMask(map.getSize() + 1, random.nextLong(), symmetryHierarchy);
            BinaryMask plateau = new BinaryMask(plateaus, random.nextLong());
//            BinaryMask accentPlateau = new BinaryMask(valleys, random.nextLong());
            BinaryMask rock = new BinaryMask(impassable, random.nextLong()).inflate(4);
            BinaryMask accentRock = new BinaryMask(slope, .75f, random.nextLong());
            BinaryMask highAltitude = new BinaryMask(heightmapBase, waterHeight + 25f, random.nextLong());
            FloatMask groundTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask accentGroundTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask highlightGroundTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask plateauTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask accentPlateauTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask rockTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask accentRockTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);
            FloatMask highAltitudeTexture = new FloatMask(map.getSize() / 2, random.nextLong(), symmetryHierarchy);

            ground.shrink(map.getSize() / 4).erode(.5f, symmetryHierarchy.getSpawnSymmetry(), 6).grow(.5f, symmetryHierarchy.getSpawnSymmetry(), 4);
            ground.combine(plateaus).intersect(land).smooth(2, .25f).filterShapes(32);
//            accentGround.acid(.1f, 0).erode(.5f, symmetryHierarchy.getSpawnSymmetry()).smooth(8, .75f);
            highlightGround.combine(ground).minus(plateaus.copy().outline()).deflate(8).acid(.1f, 0).erode(.5f, symmetryHierarchy.getSpawnSymmetry()).smooth(8, .75f);
            plateau.acid(.1f, 0).erode(.5f, symmetryHierarchy.getSpawnSymmetry()).smooth(8, .75f);
//            accentPlateau.acid(.1f, 0).erode(.5f, symmetryHierarchy.getSpawnSymmetry()).smooth(8, .75f);

            groundTexture.init(ground, 0, 1).smooth(8);
//            accentGroundTexture.init(accentGround, 0, 1).smooth(8);
            highlightGroundTexture.init(highlightGround, 0, 1).smooth(8);
            plateauTexture.init(plateau, 0, 1).smooth(8);
//            accentPlateauTexture.init(accentPlateau, 0, 1).smooth(8);
            rockTexture.init(rock, 0, 1).smooth(2);
            accentRockTexture.init(accentRock, 0, 1).smooth(4);
            highAltitudeTexture.init(highAltitude, 0, 1).smooth(16);

            map.setTextureMasksLow(groundTexture, accentGroundTexture, highlightGroundTexture, plateauTexture);
            map.setTextureMasksHigh(accentPlateauTexture, rockTexture, accentRockTexture, highAltitudeTexture);
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
