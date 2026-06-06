package com.faforever.neroxis.util;

import com.faforever.neroxis.map.PositionedObject;
import com.faforever.neroxis.map.Prop;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.map.Unit;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.mask.PrimitiveMask;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MapSymmetryTester {

    private MapSymmetryTester() {}

    public static Result evaluate(SCMap map, SymmetrySettings symmetrySettings) {
        FloatMask heightMask = new FloatMask(map.getHeightmap(), null, symmetrySettings, map.getHeightMapScale(),
                                             "heightMask");
        boolean oddVsEven = evaluateOddEven(map, heightMask);
        float terrainScore = evaluateTerrain(heightMask);
        float spawnScore = evaluateSpawns(map, heightMask);
        float mexScore = evaluateMexes(map, heightMask);
        float hydroScore = evaluateHydros(map, heightMask);
        float propScore = evaluateProps(map, heightMask);
        float unitScore = evaluateUnits(map, heightMask);

        return new Result(oddVsEven, terrainScore, spawnScore, mexScore, hydroScore, propScore, unitScore);
    }

    private static float evaluateTerrain(FloatMask heightMask) {
        return DebugUtil.timedRun("evaluateTerrain", () -> getMaskScore(heightMask));
    }

    private static boolean evaluateOddEven(SCMap map, FloatMask heightMask) {
        return DebugUtil.timedRun("evaluateSpawns", () -> checkSpawnsOddEven(map.getSpawns(), heightMask));
    }

    private static float evaluateSpawns(SCMap map, FloatMask heightMask) {
        return DebugUtil.timedRun("evaluateSpawns",
                                  () -> getPositionedObjectScore(map.getSpawns(), heightMask));
    }

    private static float evaluateMexes(SCMap map, FloatMask heightMask) {
        return DebugUtil.timedRun("evaluateMexes",
                                  () -> getPositionedObjectScore(map.getMexes(), heightMask));
    }

    private static float evaluateHydros(SCMap map, FloatMask heightMask) {
        return DebugUtil.timedRun("evaluateHydros",
                                  () -> getPositionedObjectScore(map.getHydros(), heightMask));
    }

    private static float evaluateProps(SCMap map, FloatMask heightMask) {
        return DebugUtil.timedRun("evaluateProps", () -> (float) map.getProps()
                                                                    .stream()
                                                                    .collect(Collectors.groupingBy(
                                                                            Prop::getPath))
                                                                    .values()
                                                                    .stream()
                                                                    .mapToDouble(
                                                                            props -> getPositionedObjectScore(
                                                                                    props, heightMask))
                                                                    .sum());
    }

    private static float evaluateUnits(SCMap map, FloatMask heightMask) {
        return DebugUtil.timedRun("evaluateUnits", () -> (float) map.getArmies()
                                                                    .stream()
                                                                    .flatMap(army -> army.getGroups()
                                                                                         .stream()
                                                                                         .flatMap(
                                                                                                 group -> group.getUnits()
                                                                                                               .stream()
                                                                                                               .collect(
                                                                                                                       Collectors.groupingBy(
                                                                                                                               Unit::getType))
                                                                                                               .values()
                                                                                                               .stream()))
                                                                    .mapToDouble(
                                                                            units -> getPositionedObjectScore(
                                                                                    units, heightMask))
                                                                    .sum());
    }

    private static <T extends PrimitiveMask<?, T>> float getMaskScore(T mask) {
        T maskCopy = mask.copy();
        maskCopy.forceSymmetry(SymmetryType.SPAWN, false);
        float totalError;
        switch (mask) {
            case BooleanMask booleanMask -> {
                ((BooleanMask) maskCopy).subtract(booleanMask);
                totalError = (float) ((BooleanMask) maskCopy).getCount();
            }
            case FloatMask floatMask -> {
                ((FloatMask) maskCopy).subtract(floatMask).multiply((FloatMask) maskCopy);
                totalError = (float) StrictMath.sqrt(((FloatMask) maskCopy).getSum());
            }
            case IntegerMask integerMask -> {
                ((IntegerMask) maskCopy).subtract(integerMask).multiply((IntegerMask) maskCopy);
                totalError = (float) StrictMath.sqrt(((IntegerMask) maskCopy).getSum());
            }
        }
        return totalError / mask.getSize() / mask.getSize();
    }

    private static float getPositionedObjectScore(List<? extends PositionedObject> objects, Mask<?, ?> mask) {
        if (objects.isEmpty()) {
            return 0;
        }

        float locationScore = 0f;
        List<Vector3> locations = objects.stream().map(PositionedObject::getPosition).collect(Collectors.toList());
        Set<Vector3> locationsSet = Collections.newSetFromMap(new IdentityHashMap<>());
        locationsSet.addAll(locations);
        while (!locationsSet.isEmpty()) {
            Vector3 location = locations.removeFirst();
            for (Vector2 symmetryPoint : mask.getSymmetryPointsWithOutOfBounds(location, SymmetryType.SPAWN)) {
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
        }
        return locationScore / (objects.size() / 2f);
    }

    private static boolean checkSpawnsOddEven(List<Spawn> spawns, Mask<?, ?> mask) {
        for (Spawn spawn : spawns) {
            Spawn closestSpawn = null;
            float minDist = (float) StrictMath.sqrt(mask.getSize() * mask.getSize());
            Vector2 symmetrySpawn = mask.getSymmetryPoints(spawn.getPosition(), SymmetryType.SPAWN).getFirst();
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

    public record Result(
            boolean oddVsEven,
            float terrainScore,
            float spawnScore,
            float mexScore,
            float hydroScore,
            float propScore,
            float unitScore
    ) {
        public boolean isSymmetric() {
            return oddVsEven &&
                   terrainScore == 0 &&
                   spawnScore == 0 &&
                   mexScore == 0 &&
                   hydroScore == 0 &&
                   propScore == 0 &&
                   unitScore == 0;
        }
    }
}
