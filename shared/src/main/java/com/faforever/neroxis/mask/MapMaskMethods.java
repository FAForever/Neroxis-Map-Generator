package com.faforever.neroxis.mask;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.annotations.GraphParameter;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Spawn;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MapMaskMethods {

    private MapMaskMethods() {
    }

    @GraphMethod
    @GraphParameter(name = "map", value = "map")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask connectTeams(SCMap map, long seed, BooleanMask executor, int minMiddlePoints, int maxMiddlePoints, int numConnections, float maxStepSize) {
        Random random = new Random(seed);
        List<Spawn> startTeamSpawns = map.getSpawns().stream().filter(spawn -> spawn.getTeamID() == 0).collect(Collectors.toList());
        for (int i = 0; i < numConnections; ++i) {
            Spawn startSpawn = startTeamSpawns.get(random.nextInt(startTeamSpawns.size()));
            int numMiddlePoints;
            if (maxMiddlePoints > minMiddlePoints) {
                numMiddlePoints = random.nextInt(maxMiddlePoints - minMiddlePoints) + minMiddlePoints;
            } else {
                numMiddlePoints = maxMiddlePoints;
            }
            Vector2 start = new Vector2(startSpawn.getPosition());
            Vector2 end = new Vector2(start);
            float maxMiddleDistance = start.getDistance(end);
            executor.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2, (float) (StrictMath.PI / 2), SymmetryType.SPAWN);
        }
        return executor;
    }

    @GraphMethod
    @GraphParameter(name = "map", value = "map")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask connectTeamsAroundCenter(SCMap map, long seed, BooleanMask executor, int minMiddlePoints, int maxMiddlePoints, int numConnections, float maxStepSize,
                                                       int bound) {
        Random random = new Random(seed);
        List<Spawn> startTeamSpawns = map.getSpawns().stream().filter(spawn -> spawn.getTeamID() == 0).collect(Collectors.toList());
        for (int i = 0; i < numConnections; ++i) {
            Spawn startSpawn = startTeamSpawns.get(random.nextInt(startTeamSpawns.size()));
            int numMiddlePoints;
            if (maxMiddlePoints > minMiddlePoints) {
                numMiddlePoints = random.nextInt(maxMiddlePoints - minMiddlePoints) + minMiddlePoints;
            } else {
                numMiddlePoints = maxMiddlePoints;
            }
            Vector2 start = new Vector2(startSpawn.getPosition());
            Vector2 end = new Vector2(start);
            float offCenterAngle = (float) (StrictMath.PI * (1f / 3f + random.nextFloat() / 3f));
            offCenterAngle *= random.nextBoolean() ? 1 : -1;
            offCenterAngle += start.angleTo(new Vector2(executor.getSize() / 2f, executor.getSize() / 2f));
            end.addPolar(offCenterAngle, random.nextFloat() * executor.getSize() / 2f + executor.getSize() / 2f);
            end.clampMax(executor.getSize() - bound).clampMin(bound);
            float maxMiddleDistance = start.getDistance(end);
            executor.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2, (float) (StrictMath.PI / 2), SymmetryType.SPAWN);
        }
        return executor;
    }

    @GraphMethod
    @GraphParameter(name = "map", value = "map")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask connectTeammates(SCMap map, long seed, BooleanMask executor, int maxMiddlePoints, int numConnections, float maxStepSize) {
        Random random = new Random(seed);
        List<Spawn> startTeamSpawns = map.getSpawns().stream().filter(spawn -> spawn.getTeamID() == 0).collect(Collectors.toList());
        if (startTeamSpawns.size() > 1) {
            startTeamSpawns.forEach(startSpawn -> {
                for (int i = 0; i < numConnections; ++i) {
                    ArrayList<Spawn> otherSpawns = new ArrayList<>(startTeamSpawns);
                    otherSpawns.remove(startSpawn);
                    Spawn endSpawn = otherSpawns.get(random.nextInt(otherSpawns.size()));
                    int numMiddlePoints = random.nextInt(maxMiddlePoints);
                    Vector2 start = new Vector2(startSpawn.getPosition());
                    Vector2 end = new Vector2(endSpawn.getPosition());
                    float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
                    executor.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
                }
            });
        }
        return executor;
    }

    @GraphMethod
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask pathInCenterBounds(long seed, BooleanMask executor, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        Random random = new Random(seed);
        for (int i = 0; i < numPaths; i++) {
            Vector2 start = new Vector2(random.nextInt(executor.getSize() + 1 - bound * 2) + bound, random.nextInt(executor.getSize() + 1 - bound * 2) + bound);
            Vector2 end = new Vector2(random.nextInt(executor.getSize() + 1 - bound * 2) + bound, random.nextInt(executor.getSize() + 1 - bound * 2) + bound);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            executor.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
        return executor;
    }

    @GraphMethod
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask pathInEdgeBounds(long seed, BooleanMask executor, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        Random random = new Random(seed);
        for (int i = 0; i < numPaths; i++) {
            int startX = random.nextInt(bound) + (random.nextBoolean() ? 0 : executor.getSize() - bound);
            int startY = random.nextInt(bound) + (random.nextBoolean() ? 0 : executor.getSize() - bound);
            int endX = random.nextInt(bound * 2) - bound + startX;
            int endY = random.nextInt(bound * 2) - bound + startY;
            Vector2 start = new Vector2(startX, startY);
            Vector2 end = new Vector2(endX, endY);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            executor.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
        return executor;
    }

    @GraphMethod
    @GraphParameter(name = "map", value = "map")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask pathAroundSpawns(SCMap map, long seed, BooleanMask executor, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        Random random = new Random(seed);
        map.getSpawns().forEach(spawn -> {
            Vector2 start = new Vector2(spawn.getPosition());
            for (int i = 0; i < numPaths; i++) {
                int endX = (int) (random.nextFloat() * bound + start.getX());
                int endY = (int) (random.nextFloat() * bound + start.getY());
                Vector2 end = new Vector2(endX, endY);
                int numMiddlePoints = random.nextInt(maxMiddlePoints);
                float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
                executor.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
            }
        });
        return executor;
    }

    @GraphMethod
    @GraphParameter(name = "map", value = "map")
    public static BooleanMask fillSpawnCircle(SCMap map, BooleanMask executor, float radius) {
        map.getSpawns().forEach(spawn -> {
            Vector3 location = spawn.getPosition();
            executor.fillCircle(location, radius, true);
        });
        return executor;
    }

    @GraphMethod
    @GraphParameter(name = "seed", value = "random.nextLong()")
    @GraphParameter(name = "map", value = "map")
    public static BooleanMask fillSpawnCircleWithProbability(SCMap map, long seed, BooleanMask executor, float spawnSize, float probability) {
        Random random = new Random(seed);
        if (random.nextFloat() < probability) {
            map.getSpawns().forEach(spawn -> {
                Vector3 location = spawn.getPosition();
                executor.fillCircle(location, spawnSize, true);
            });
        }
        return executor;
    }
}