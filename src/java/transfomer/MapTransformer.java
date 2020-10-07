package transfomer;

import export.SCMapExporter;
import export.SaveExporter;
import importer.SCMapImporter;
import importer.SaveImporter;
import map.FloatMask;
import map.SCMap;
import map.Symmetry;
import map.SymmetryHierarchy;
import util.ArgumentParser;
import util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

public strictfp class MapTransformer {

    public static boolean DEBUG = false;

    private Path inMapPath;
    private Path outFolderPath;
    private String mapName;
    private SCMap map;

    //masks used in transformation
    private FloatMask heightmapBase;

    private boolean transformSpawns;
    private boolean transformResources;
    private boolean transformProps;
    private boolean transformTerrain;
    private SymmetryHierarchy symmetryHierarchy;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);
        if (DEBUG) {
            Path debugDir = Paths.get(".", "debug");
            FileUtils.deleteRecursiveIfExists(debugDir);
            Files.createDirectory(debugDir);
        }

        MapTransformer transformer = new MapTransformer();

        transformer.interpretArguments(args);

        System.out.println("Transforming map " + transformer.inMapPath);
        transformer.importMap();
        transformer.transform();
        transformer.exportMap();
        System.out.println("Saving map to " + transformer.outFolderPath.toAbsolutePath());
        System.out.println("Terrain Symmetry: " + transformer.symmetryHierarchy.getTerrainSymmetry());
        System.out.println("Team Symmetry: " + transformer.symmetryHierarchy.getTeamSymmetry());
        System.out.println("Spawn Symmetry: " + transformer.symmetryHierarchy.getSpawnSymmetry());
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
                    "--symmetry arg         required, set the symmetry for the map(X, Y, XY, YX, POINT)\n" +
                    "--base arg             required, set which half to use as base for forced symmetry (TOP, BOTTOM, LEFT, RIGHT)\n" +
                    "--spawns               optional, force spawn symmetry\n" +
                    "--resources            optional, force mex symmetry\n" +
                    "--props                optional, force prop symmetry\n" +
                    "--wrecks               optional, force wreck symmetry\n" +
                    "--terrain              optional, force terrain symmetry\n" +
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
        symmetryHierarchy = new SymmetryHierarchy(Symmetry.valueOf(arguments.get("symmetry")), Symmetry.valueOf(arguments.get("symmetry")));
        symmetryHierarchy.setSpawnSymmetry(Symmetry.valueOf(arguments.get("symmetry")));
        transformSpawns = arguments.containsKey("spawns");
        transformResources = arguments.containsKey("resources");
        transformProps = arguments.containsKey("props");
        transformTerrain = arguments.containsKey("terrain");
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
            Files.copy(inMapPath.resolve(mapName + "_scenario.lua"), outFolderPath.resolve(mapName).resolve(mapName + "_scenario.lua"));
            Files.copy(inMapPath.resolve(mapName + "_script.lua"), outFolderPath.resolve(mapName).resolve(mapName + "_script.lua"));
            SCMapExporter.exportSCMAP(outFolderPath, mapName, map);
            SaveExporter.exportSave(outFolderPath, mapName, map);
            System.out.printf("File export done: %d ms\n", System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while saving the map.");
        }
    }

    public void transform() {
        boolean waterPresent = map.getBiome().getWaterSettings().isWaterPresent();
        float waterHeight;
        if (waterPresent) {
            waterHeight = map.getBiome().getWaterSettings().getElevation();
        } else {
            waterHeight = 0;
        }
        heightmapBase = map.getHeightMask(symmetryHierarchy);

    }
}
