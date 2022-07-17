package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.RequiredMapPathMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.importer.MapImporter;
import com.faforever.neroxis.map.*;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import picocli.CommandLine;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static picocli.CommandLine.*;

@Command(name = "evaluate", mixinStandardHelpOptions = true, description = "Evaluates a map's symmetry error. Higher values represent greater asymmetry", versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public strictfp class MapEvaluator implements Callable<Integer> {
    float terrainScore;
    float spawnScore;
    float propScore;
    float mexScore;
    float hydroScore;
    float unitScore;
    boolean oddVsEven;
    @Spec
    private CommandLine.Model.CommandSpec spec;
    @Mixin
    private RequiredMapPathMixin requiredMapPathMixin;
    private SCMap map;
    private FloatMask heightMask;

    private static <T extends Mask<?, T>> float getMaskScore(T mask) {
        String visualName = "diff" + mask.getVisualName();
        T maskCopy = mask.copy();
        maskCopy.forceSymmetry(SymmetryType.SPAWN, false);
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
        if (DebugUtil.DEBUG) {
            maskCopy.startVisualDebugger(visualName).show();
        }
        return totalError / mask.getSize() / mask.getSize();
    }

    private static float getPositionedObjectScore(List<? extends PositionedObject> objects, Mask<?, ?> mask) {
        if (objects.size() == 0) {
            return 0;
        }

        float locationScore = 0f;
        List<Vector3> locations = objects.stream().map(PositionedObject::getPosition).collect(Collectors.toList());
        Set<Vector3> locationsSet = new HashSet<>(locations);
        while (locationsSet.size() > 0) {
            Vector3 location = locations.remove(0);
            Vector2 symmetryPoint = mask.getSymmetryPointsWithOutOfBounds(location, SymmetryType.SPAWN).get(0);
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

    private static boolean checkSpawnsOddEven(List<Spawn> spawns, Mask<?, ?> mask) {
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

    @Option(names = "--debug", description = "Turn on debugging mode")
    public void setDebugging(boolean debug) {
        DebugUtil.DEBUG = debug;
    }

    @Override
    public Integer call() {
        System.out.printf("Evaluating map %s%n", requiredMapPathMixin.getMapPath());
        importMap();
        evaluate();
        System.out.println("Done");
        return 0;
    }

    private void importMap() {
        try {
            map = MapImporter.importMap(requiredMapPathMixin.getMapPath());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while importing the map.");
        }
    }

    private void evaluate() {
        List<Symmetry> symmetries = Arrays.stream(Symmetry.values())
                .filter(symmetry -> symmetry.getNumSymPoints() == 2)
                .collect(Collectors.toList());
        for (Symmetry symmetry : symmetries) {
            SymmetrySettings symmetrySettings = new SymmetrySettings(symmetry);
            heightMask = new FloatMask(map.getHeightmap(), null, symmetrySettings, map.getHeightMapScale(),
                    "heightMask");
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

    private void evaluateTerrain() {
        DebugUtil.timedRun("evaluateTerrain", () -> terrainScore = getMaskScore(heightMask));
    }

    private void evaluateSpawns() {
        DebugUtil.timedRun("evaluateSpawns", () -> {
            spawnScore = getPositionedObjectScore(map.getSpawns(), heightMask);
            oddVsEven = checkSpawnsOddEven(map.getSpawns(), heightMask);
        });
    }

    private void evaluateMexes() {
        DebugUtil.timedRun("evaluateMexes", () -> mexScore = getPositionedObjectScore(map.getMexes(), heightMask));
    }

    private void evaluateHydros() {
        DebugUtil.timedRun("evaluateHydros", () -> hydroScore = getPositionedObjectScore(map.getHydros(), heightMask));
    }

    private void evaluateProps() {
        DebugUtil.timedRun("evaluateProps", () -> propScore = (float) map.getProps().stream().collect(Collectors.groupingBy(Prop::getPath))
                .values().stream().mapToDouble(props -> getPositionedObjectScore(props, heightMask))
                .sum());
    }

    private void evaluateUnits() {
        DebugUtil.timedRun("evaluateUnits", () -> unitScore = (float) map.getArmies().stream()
                .flatMap(army -> army.getGroups().stream().flatMap(group -> group.getUnits().stream()
                        .collect(Collectors.groupingBy(Unit::getType)).values().stream()))
                .mapToDouble(units -> getPositionedObjectScore(units, heightMask))
                .sum());
    }
}
