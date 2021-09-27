package com.faforever.neroxis.map.evaluator;

import com.faforever.neroxis.map.PositionedObject;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetrySource;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.map.importer.MapImporter;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.ArgumentParser;
import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public strictfp class MapEvaluator {

    public static boolean DEBUG = false;

    private Path inMapPath;
    private Path outFolderPath;
    private SCMap map;

    private IntegerMask heightMask;
    private SymmetrySettings symmetrySettings;
    private boolean reverseSide;

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
//        com.faforever.neroxis.map.evaluator.saveReport();
        System.out.println("Saving report to " + evaluator.outFolderPath.toAbsolutePath());
        System.out.println("Done");
    }

    public void interpretArguments(String[] args) {
        interpretArguments(ArgumentParser.parse(args));
    }

    private void interpretArguments(Map<String, String> arguments) {
        if (arguments.containsKey("help")) {
            System.out.println("com.faforever.neroxis.map-transformer usage:\n" +
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

    public static float getPositionedObjectScore(List<? extends PositionedObject> objects, Mask<?, ?> mask) {
        float locationScore = 0f;
        List<Vector3> locations = objects.stream().map(PositionedObject::getPosition).collect(Collectors.toList());
        Set<Vector3> locationsSet = new HashSet<>(locations);
        while (locationsSet.size() > 0) {
            Vector3 location = locations.remove(0);
            Vector3 closestLoc = null;
            float minDist = (float) StrictMath.sqrt(mask.getSize() * mask.getSize());
            for (Vector3 other : locations) {
                Vector2 symmetryPoint = mask.getSymmetryPoints(other, SymmetryType.SPAWN).get(0);
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

    public static float getMaskScore(Mask<?, ?> mask) {
        String visualName = "diff" + mask.getVisualName();
        if (mask instanceof FloatMask) {
            FloatMask difference = (FloatMask) mask.copy();
            difference.startVisualDebugger(visualName);
            difference.applySymmetry(SymmetryType.SPAWN, false);
            difference.show();
            return (float) StrictMath.sqrt(difference.subtract((FloatMask) mask).multiply(difference).show().getSum());
        } else if (mask instanceof IntegerMask) {
            IntegerMask difference = (IntegerMask) mask.copy();
            difference.startVisualDebugger(visualName);
            difference.applySymmetry(SymmetryType.SPAWN, false);
            difference.show();
            return (float) StrictMath.sqrt(difference.subtract((IntegerMask) mask).multiply(difference).getSum());
        } else if (mask instanceof BooleanMask) {
            BooleanMask difference = (BooleanMask) mask.copy();
            difference.startVisualDebugger(visualName);
            difference.applySymmetry(SymmetryType.SPAWN, false);
            difference.show();
            return difference.subtract((BooleanMask) mask).getCount();
        }
        throw new IllegalArgumentException("Not a supported Mask type");
    }

    public void evaluateTerrain() {
        long sTime = System.currentTimeMillis();
        heightMask = new IntegerMask(map.getHeightmap(), null, symmetrySettings, "heightMask");
        float terrainScore = getMaskScore(heightMask);
        System.out.printf("Terrain Score: %.2f%n", terrainScore);

        IntegerMask textureMasksLowMask = new IntegerMask(map.getTextureMasksLow(), null, symmetrySettings, "textureMasksLow");
        IntegerMask textureMasksHighMask = new IntegerMask(map.getTextureMasksHigh(), null, symmetrySettings, "textureMasksHigh");
        float textureScore = 0;
        textureScore += getMaskScore(textureMasksLowMask);
        textureScore += getMaskScore(textureMasksHighMask);
        System.out.printf("Texture Score: %.2f%n", textureScore);
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateTerrain\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateSpawns() {
        long sTime = System.currentTimeMillis();
        float spawnScore = getPositionedObjectScore(map.getSpawns(), heightMask);
        System.out.printf("Spawn Score: %.2f%n", spawnScore);
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateSpawns\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateMexes() {
        long sTime = System.currentTimeMillis();
        float mexScore = getPositionedObjectScore(map.getMexes(), heightMask);
        System.out.printf("Mex Score: %.2f%n", mexScore);
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateMexes\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateHydros() {
        long sTime = System.currentTimeMillis();
        float hydroScore = getPositionedObjectScore(map.getHydros(), heightMask);
        System.out.printf("Hydro Score: %.2f%n", hydroScore);
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateHydros\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateProps() {
        long sTime = System.currentTimeMillis();
        float propScore = getPositionedObjectScore(map.getProps(), heightMask);
        System.out.printf("Prop Score: %.2f%n", propScore);
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateProps\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateUnits() {
        long sTime = System.currentTimeMillis();
        float unitScore = getPositionedObjectScore(map.getArmies().stream().flatMap(army -> army.getGroups().stream()
                .flatMap(group -> group.getUnits().stream())).collect(Collectors.toList()), heightMask);
        System.out.printf("Unit Score: %.2f%n", unitScore);
        if (DEBUG) {
            System.out.printf("Done: %4d ms, evaluateUnits\n",
                    System.currentTimeMillis() - sTime);
        }
    }
}
