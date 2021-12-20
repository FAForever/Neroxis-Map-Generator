package com.faforever.neroxis.map.evaluator;

import com.faforever.neroxis.map.PositionedObject;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.map.importer.MapImporter;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.ArgumentParser;
import com.faforever.neroxis.util.DebugUtils;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public strictfp class MapEvaluator {

    private Path inMapPath;
    private Path outFolderPath;
    private SCMap map;

    private FloatMask heightMask;
    private SymmetrySettings symmetrySettings;
    private boolean saveReport;

    float terrainScore;
    float spawnScore;
    float propScore;
    float mexScore;
    float hydroScore;
    float unitScore;
    boolean oddVsEven;

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
        if (evaluator.saveReport) {
            evaluator.saveReport();
            System.out.println("Saving report to " + evaluator.outFolderPath.toAbsolutePath());
        }
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
                    "--debug                optional, turn on debugging options\n");
            return;
        }

        if (arguments.containsKey("debug")) {
            DebugUtils.DEBUG = true;
        }

        if (!arguments.containsKey("in-folder-path")) {
            System.out.println("Input Folder not Specified");
            return;
        }

        if (arguments.containsKey("out-folder-path")) {
            saveReport = true;
            outFolderPath = Paths.get(arguments.get("out-folder-path"));
        }

        inMapPath = Paths.get(arguments.get("in-folder-path"));
    }

    public void importMap() {
        try {
            map = MapImporter.importMap(inMapPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while importing the map.");
        }
    }

    public void saveReport() {

    }

    public void evaluate() {
        List<Symmetry> symmetries = Arrays.stream(Symmetry.values()).filter(symmetry -> symmetry.getNumSymPoints() == 2).collect(Collectors.toList());
        for (Symmetry symmetry : symmetries) {
            symmetrySettings = new SymmetrySettings(symmetry);
            heightMask = new FloatMask(map.getHeightmap(), null, symmetrySettings, map.getHeightMapScale(), "heightMask");
            evaluateTerrain();
            evaluateSpawns();
            evaluateMexes();
            evaluateHydros();
            evaluateProps();
            evaluateUnits();
            System.out.println();
            System.out.printf("Spawns Odd vs Even for Symmetry %s: %s%n", symmetry, oddVsEven);
            System.out.printf("Terrain Difference for Symmetry %s: %.8f%n", symmetry, terrainScore);
            System.out.printf("Spawn Difference for Symmetry %s: %.2f%n", symmetry, spawnScore);
            System.out.printf("Mex Difference for Symmetry %s: %.2f%n", symmetry, mexScore);
            System.out.printf("Hydro Difference for Symmetry %s: %.2f%n", symmetry, hydroScore);
            System.out.printf("Prop Difference for Symmetry %s: %.2f%n", symmetry, propScore);
            System.out.printf("Unit Difference for Symmetry %s: %.2f%n", symmetry, unitScore);
        }
    }

    public static float getPositionedObjectScore(List<? extends PositionedObject> objects, Mask<?, ?> mask) {
        if (objects.size() == 0) {
            return 0;
        }

        float locationScore = 0f;
        List<Vector3> locations = objects.stream().map(PositionedObject::getPosition).collect(Collectors.toList());
        Set<Vector3> locationsSet = new HashSet<>(locations);
        while (locationsSet.size() > 0) {
            Vector3 location = locations.remove(0);
            Vector2 symmetryPoint = mask.getSymmetryPoints(location, SymmetryType.SPAWN).get(0);
            Vector3 closestLoc = null;
            float minDist = (float) StrictMath.sqrt(mask.getSize() * mask.getSize());
            for (Vector3 other : locations) {
                float dist = other.getXZDistance(symmetryPoint);
                if (dist < minDist) {
                    closestLoc = other;
                    minDist = dist;
                }
            }
            locationsSet.remove(location);
            if (closestLoc != null) {
                locationsSet.remove(closestLoc);
            }
            locationScore += minDist;
            locations = new ArrayList<>(locationsSet);
        }
        return locationScore / (objects.size() / 2f);
    }

    public static boolean checkSpawnsOddEven(List<Spawn> spawns, Mask<?, ?> mask) {
        for (Spawn spawn : spawns) {
            Spawn closestSpawn = null;
            float minDist = (float) StrictMath.sqrt(mask.getSize() * mask.getSize());
            Vector2 symmetrySpawn = mask.getSymmetryPoints(spawn.getPosition(), SymmetryType.SPAWN).get(0);
            for (Spawn otherSpawn : spawns) {
                if (!otherSpawn.equals(spawn)) {
                    float dist = otherSpawn.getPosition().getXZDistance(symmetrySpawn);
                    if (dist < minDist) {
                        closestSpawn = otherSpawn;
                        minDist = dist;
                    }
                }
            }
            if (closestSpawn == null) {
                return false;
            }
            int spawnId = Integer.parseInt(spawn.getId().split("_")[1]);
            int closestSpawnId = Integer.parseInt(closestSpawn.getId().split("_")[1]);
            if (spawnId % 2 == 0) {
                if (spawnId != (closestSpawnId + 1)) {
                    return false;
                }
            } else {
                if (spawnId != (closestSpawnId - 1)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static <T extends Mask<?, T>> float getMaskScore(T mask) {
        String visualName = "diff" + mask.getVisualName();
        T maskCopy = mask.copy();
        maskCopy.applySymmetry(SymmetryType.SPAWN, false);
        float totalError;
        if (mask instanceof BooleanMask) {
            ((BooleanMask) maskCopy).subtract((BooleanMask) mask);
            totalError = (float) ((BooleanMask) maskCopy).getCount();
        } else if (mask instanceof FloatMask) {
            ((FloatMask) maskCopy).subtract((FloatMask) mask).multiply((FloatMask) maskCopy);
            totalError = (float) StrictMath.sqrt(((FloatMask) maskCopy).getSum());
        } else if (mask instanceof IntegerMask) {
            ((IntegerMask) maskCopy).subtract((IntegerMask) mask).multiply((IntegerMask) maskCopy);
            totalError = (float) StrictMath.sqrt(((IntegerMask) maskCopy).getSum());
        } else {
            throw new IllegalArgumentException("Not a supported Mask type");
        }
        if (DebugUtils.DEBUG) {
            maskCopy.startVisualDebugger(visualName).show();
        }
        return totalError / mask.getSize() / mask.getSize();
    }

    public void evaluateTerrain() {
        long sTime = System.currentTimeMillis();
        terrainScore = getMaskScore(heightMask);
        if (DebugUtils.DEBUG) {
            System.out.printf("Done: %4d ms, evaluateTerrain\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateSpawns() {
        long sTime = System.currentTimeMillis();
        spawnScore = getPositionedObjectScore(map.getSpawns(), heightMask);
        oddVsEven = checkSpawnsOddEven(map.getSpawns(), heightMask);
        if (DebugUtils.DEBUG) {
            System.out.printf("Done: %4d ms, evaluateSpawns\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateMexes() {
        long sTime = System.currentTimeMillis();
        mexScore = getPositionedObjectScore(map.getMexes(), heightMask);
        if (DebugUtils.DEBUG) {
            System.out.printf("Done: %4d ms, evaluateMexes\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateHydros() {
        long sTime = System.currentTimeMillis();
        hydroScore = getPositionedObjectScore(map.getHydros(), heightMask);
        if (DebugUtils.DEBUG) {
            System.out.printf("Done: %4d ms, evaluateHydros\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateProps() {
        long sTime = System.currentTimeMillis();
        propScore = getPositionedObjectScore(map.getProps(), heightMask);
        if (DebugUtils.DEBUG) {
            System.out.printf("Done: %4d ms, evaluateProps\n",
                    System.currentTimeMillis() - sTime);
        }
    }

    public void evaluateUnits() {
        long sTime = System.currentTimeMillis();
        unitScore = getPositionedObjectScore(map.getArmies().stream().flatMap(army -> army.getGroups().stream()
                .flatMap(group -> group.getUnits().stream())).collect(Collectors.toList()), heightMask);
        if (DebugUtils.DEBUG) {
            System.out.printf("Done: %4d ms, evaluateUnits\n",
                    System.currentTimeMillis() - sTime);
        }
    }
}
