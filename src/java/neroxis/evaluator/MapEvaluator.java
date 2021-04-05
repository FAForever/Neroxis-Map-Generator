package neroxis.evaluator;

import neroxis.importer.MapImporter;
import neroxis.map.*;
import neroxis.util.ArgumentParser;
import neroxis.util.Vector2f;
import neroxis.util.Vector3f;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public strictfp class MapEvaluator {

    public static boolean DEBUG = false;

    private Path inMapPath;
    private Path outFolderPath;
    private SCMap map;

    private FloatMask heightmapBase;
    private SymmetrySettings symmetrySettings;
    private boolean reverseSide;
    private float textureScore;
    private float terrainScore;
    private float spawnScore;
    private float mexScore;
    private float hydroScore;
    private float unitScore;
    private float propScore;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.ROOT);

        MapEvaluator evaluator = new MapEvaluator();

        evaluator.interpretArguments(args);
        if (evaluator.inMapPath == null) {
            return;
        }

        System.out.println("Evaluating map " + evaluator.inMapPath);
        evaluator.importMap();
        evaluator.evaluate();
//        neroxis.evaluator.saveReport();
        System.out.println("Saving report to " + evaluator.outFolderPath.toAbsolutePath());
        System.out.println("Done");
    }

    public void interpretArguments(String[] args) {
        interpretArguments(ArgumentParser.parse(args));
    }

    private void interpretArguments(Map<String, String> arguments) {
        if (arguments.containsKey("help")) {
            System.out.println("neroxis.map-transformer usage:\n" +
                    "--help                 produce help message\n" +
                    "--in-folder-path arg   required, set the input folder for the map\n" +
                    "--out-folder-path arg  required, set the output folder for the symmetry report\n" +
                    "--symmetry arg         required, set the symmetry for the map(X, Z, XZ, ZX, POINT)\n" +
                    "--source arg           required, set which half to use as reference for evaluation (TOP, BOTTOM, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT)\n" +
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

        if (!arguments.containsKey("symmetry")) {
            System.out.println("Symmetry not Specified");
            return;
        }

        if (!arguments.containsKey("source")) {
            System.out.println("Source not Specified");
            return;
        }

        inMapPath = Paths.get(arguments.get("in-folder-path"));
        outFolderPath = Paths.get(arguments.get("out-folder-path"));
        Symmetry teamSymmetry;
        switch (SymmetrySource.valueOf(arguments.get("source"))) {
            case TOP:
                teamSymmetry = Symmetry.Z;
                reverseSide = false;
                break;
            case BOTTOM:
                teamSymmetry = Symmetry.Z;
                reverseSide = true;
                break;
            case LEFT:
                teamSymmetry = Symmetry.X;
                reverseSide = false;
                break;
            case RIGHT:
                teamSymmetry = Symmetry.X;
                reverseSide = true;
                break;
            case TOP_LEFT:
                teamSymmetry = Symmetry.ZX;
                reverseSide = false;
                break;
            case TOP_RIGHT:
                teamSymmetry = Symmetry.XZ;
                reverseSide = false;
                break;
            case BOTTOM_LEFT:
                teamSymmetry = Symmetry.XZ;
                reverseSide = true;
                break;
            case BOTTOM_RIGHT:
                teamSymmetry = Symmetry.ZX;
                reverseSide = true;
                break;
            default:
                teamSymmetry = Symmetry.NONE;
                reverseSide = false;
                break;
        }
        symmetrySettings = new SymmetrySettings(Symmetry.valueOf(arguments.get("symmetry")), teamSymmetry, Symmetry.valueOf(arguments.get("symmetry")));
    }

    public void importMap() {
        try {
            map = MapImporter.importMap(inMapPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while importing the map.");
        }
    }

//    public void saveReport() {
//        try {
//            long startTime = System.currentTimeMillis();
//
//
//
//            System.out.printf("Report write done: %d ms\n", System.currentTimeMillis() - startTime);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.err.println("Error while saving the map.");
//        }
//    }

    public void evaluate() {
        evaluateTerrain();
        evaluateSpawns();
        evaluateMexes();
        evaluateHydros();
        evaluateProps();
        evaluateUnits();
    }

    public static float getLocationListScore(List<Vector3f> locations, Mask<?> mask) {
        float locationScore = 0f;
        Set<Vector3f> locationsSet = new HashSet<>(locations);
        while (locationsSet.size() > 0) {
            Vector3f location = locations.remove(0);
            Vector3f closestLoc = null;
            float minDist = (float) StrictMath.sqrt(mask.getSize() * mask.getSize());
            for (Vector3f other : locations) {
                Vector2f symmetryPoint = mask.getSymmetryPoints(other, SymmetryType.SPAWN).get(0);
                float dist = location.getXZDistance(symmetryPoint);
                if (dist < minDist) {
                    closestLoc = other;
                    minDist = dist;
                }
            }
            locationsSet.remove(location);
            locationsSet.remove(closestLoc);
            locationScore += minDist;
            locations = new ArrayList<>(locationsSet);
        }
        return locationScore;
    }

    public static float getMaskScore(Mask<?> mask) {
        if (mask instanceof FloatMask) {
            FloatMask difference = (FloatMask) mask.copy();
            difference.startVisualDebugger("diff");
            difference.applySymmetry(SymmetryType.SPAWN, false);
            difference.show();
            return (float) StrictMath.sqrt(difference.subtract((FloatMask) mask).multiply(difference).getSum());
        } else if (mask instanceof BinaryMask) {
            BinaryMask difference = (BinaryMask) mask.copy();
            difference.startVisualDebugger("diff");
            difference.applySymmetry(SymmetryType.SPAWN, false);
            difference.show();
            return difference.minus((BinaryMask) mask).getCount();
        }
        throw new IllegalArgumentException("Not a supported Mask type");
    }

    public void evaluateTerrain() {
        long sTime = System.currentTimeMillis();
        heightmapBase = map.getHeightMask(symmetrySettings);
        terrainScore = getMaskScore(heightmapBase);
        System.out.println(String.format("Terrain Score: %.2f", terrainScore));

        FloatMask[] texturesMasks = map.getTextureMasksRaw(symmetrySettings);
        textureScore = 0;
        for (FloatMask textureMask : texturesMasks) {
            textureScore += getMaskScore(textureMask);
        }
        System.out.println(String.format("Texture Score: %.2f", textureScore));
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateTerrain\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateSpawns() {
        long sTime = System.currentTimeMillis();
        spawnScore = getLocationListScore(map.getSpawns().stream().map(Spawn::getPosition).collect(Collectors.toList()), heightmapBase);
        System.out.println(String.format("Spawn Score: %.2f", spawnScore));
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateSpawns\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateMexes() {
        long sTime = System.currentTimeMillis();
        mexScore = getLocationListScore(map.getMexes().stream().map(Marker::getPosition).collect(Collectors.toList()), heightmapBase);
        System.out.println(String.format("Mex Score: %.2f", mexScore));
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateMexes\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateHydros() {
        long sTime = System.currentTimeMillis();
        hydroScore = getLocationListScore(map.getHydros().stream().map(Marker::getPosition).collect(Collectors.toList()), heightmapBase);
        System.out.println(String.format("Hydro Score: %.2f", hydroScore));
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateHydros\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateProps() {
        long sTime = System.currentTimeMillis();
        propScore = getLocationListScore(map.getProps().stream().map(Prop::getPosition).collect(Collectors.toList()), heightmapBase);
        System.out.println(String.format("Prop Score: %.2f", propScore));
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateProps\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateUnits() {
        long sTime = System.currentTimeMillis();
        unitScore = getLocationListScore(map.getArmies().stream().flatMap(army -> army.getGroups().stream()
                .flatMap(group -> group.getUnits().stream())).map(Unit::getPosition).collect(Collectors.toList()), heightmapBase);
        System.out.println(String.format("Unit Score: %.2f", unitScore));
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateUnits\n",
                    System.currentTimeMillis() - sTime);
        }
    }
}
