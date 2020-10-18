package evaluator;

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

public strictfp class MapEvaluator {

    public static boolean DEBUG = false;

    private Path inMapPath;
    private Path outFolderPath;
    private String mapName;
    private String mapFolder;
    private SCMap map;

    private SymmetryHierarchy symmetryHierarchy;
    private boolean reverseSide;
    private float textureScore;
    private float terrainScore;
    private float mexScore;
    private float hydroScore;
    private float wreckScore;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);
        if (DEBUG) {
            Path debugDir = Paths.get(".", "debug");
            FileUtils.deleteRecursiveIfExists(debugDir);
            Files.createDirectory(debugDir);
        }

        MapEvaluator evaluator = new MapEvaluator();

        evaluator.interpretArguments(args);

        System.out.println("Evaluating map " + evaluator.inMapPath);
        evaluator.importMap();
        evaluator.evaluate();
//        evaluator.saveReport();
        System.out.println("Saving report to " + evaluator.outFolderPath.toAbsolutePath());
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
                    "--out-folder-path arg  required, set the output folder for the symmetry report\n" +
                    "--symmetry arg         required, set the symmetry for the map(X, Z, XZ, ZX, POINT)\n" +
                    "--source arg           required, set which half to use as reference for evaluation (TOP, BOTTOM, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT)\n" +
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

        if (!arguments.containsKey("symmetry")) {
            System.out.println("Symmetry not Specified");
            System.exit(3);
        }

        if (!arguments.containsKey("source")) {
            System.out.println("Source not Specified");
            System.exit(4);
        }

        inMapPath = Paths.get(arguments.get("in-folder-path"));
        outFolderPath = Paths.get(arguments.get("out-folder-path"));
        Symmetry teamSymmetry;
        switch (SymmetrySource.valueOf(arguments.get("source"))) {
            case TOP -> {
                teamSymmetry = Symmetry.Z;
                reverseSide = false;
            }
            case BOTTOM -> {
                teamSymmetry = Symmetry.Z;
                reverseSide = true;
            }
            case LEFT -> {
                teamSymmetry = Symmetry.X;
                reverseSide = false;
            }
            case RIGHT -> {
                teamSymmetry = Symmetry.X;
                reverseSide = true;
            }
            case TOP_LEFT -> {
                teamSymmetry = Symmetry.ZX;
                reverseSide = false;
            }
            case TOP_RIGHT -> {
                teamSymmetry = Symmetry.XZ;
                reverseSide = false;
            }
            case BOTTOM_LEFT -> {
                teamSymmetry = Symmetry.XZ;
                reverseSide = true;
            }
            case BOTTOM_RIGHT -> {
                teamSymmetry = Symmetry.ZX;
                reverseSide = true;
            }
            default -> {
                teamSymmetry = Symmetry.NONE;
                reverseSide = false;
            }
        }
        symmetryHierarchy = new SymmetryHierarchy(Symmetry.valueOf(arguments.get("symmetry")), teamSymmetry);
        symmetryHierarchy.setSpawnSymmetry(Symmetry.valueOf(arguments.get("symmetry")));
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

//    public void saveReport() {
//        try {
//            long startTime = System.currentTimeMillis();
//
//            System.out.printf("Report write done: %d ms\n", System.currentTimeMillis() - startTime);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.err.println("Error while saving the map.");
//        }
//    }

    public void evaluate() {
        //masks used in evaluation
        FloatMask heightmapBase = map.getHeightMask(symmetryHierarchy);
        float terrainScore = getFloatMaskScore(heightmapBase);

        FloatMask[] texturesMasks = map.getTextureMasksScaled(symmetryHierarchy);
        float textureScore = 0;
        for (FloatMask textureMask : texturesMasks) {
            textureScore += getFloatMaskScore(textureMask);
        }

    }

    public float getFloatMaskScore(FloatMask mask) {
        FloatMask difference = mask.copy();
        difference.applySymmetry(reverseSide);
        return (float) StrictMath.sqrt(difference.subtract(mask).multiply(difference).getSum());
    }
}
