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

public class MapMaskMethods {
    private MapMaskMethods() {
    }

    @GraphMethod
    @GraphParameter(name = "map", value = "map")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask connectTeams(SCMap map, long seed, BooleanMask exec, int minMiddlePoints,
                                           int maxMiddlePoints, int numConnections, float maxStepSize) {
        Random random = new Random(seed);
        List<Spawn> startTeamSpawns = map.getSpawns()
                                         .stream()
                                         .filter(spawn -> spawn.getTeamID() == 0)
                                         .toList();
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
            exec.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2,
                         (float) (StrictMath.PI / 2), SymmetryType.SPAWN);
        }
        return exec;
    }

    @GraphMethod
    @GraphParameter(name = "map", value = "map")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask connectTeamsAroundCenter(SCMap map, long seed, BooleanMask exec, int minMiddlePoints,
                                                       int maxMiddlePoints, int numConnections, float maxStepSize,
                                                       int bound) {
        List<Spawn> startTeamSpawns = map.getSpawns()
                                         .stream()
                                         .filter(spawn -> spawn.getTeamID() == 0)
                                         .toList();
        return exec.enqueue(() -> {
            Random random = new Random(seed);
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
                offCenterAngle += start.angleTo(new Vector2(exec.getSize() / 2f, exec.getSize() / 2f));
                end.addPolar(offCenterAngle, random.nextFloat() * exec.getSize() / 2f + exec.getSize() / 2f);
                end.clampMax(exec.getSize() - bound).clampMin(bound);
                float maxMiddleDistance = start.getDistance(end);
                exec.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2,
                             (float) (StrictMath.PI / 2), SymmetryType.SPAWN);
            }
        });
    }

    @GraphMethod
    @GraphParameter(name = "map", value = "map")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask connectTeammates(SCMap map, long seed, BooleanMask exec, int maxMiddlePoints,
                                               int numConnections, float maxStepSize) {
        List<Spawn> startTeamSpawns = map.getSpawns()
                                         .stream()
                                         .filter(spawn -> spawn.getTeamID() == 0)
                                         .toList();
        return exec.enqueue(() -> {
            Random random = new Random(seed);
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
                        exec.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0,
                                  (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
                    }
                });
            }
        });
    }

    @GraphMethod
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask pathInCenterBounds(long seed, BooleanMask exec, float maxStepSize, int numPaths,
                                                 int maxMiddlePoints, int bound, float maxAngleError) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            for (int i = 0; i < numPaths; i++) {
                Vector2 start = new Vector2(random.nextInt(exec.getSize() + 1 - bound * 2) + bound,
                                            random.nextInt(exec.getSize() + 1 - bound * 2) + bound);
                Vector2 end = new Vector2(random.nextInt(exec.getSize() + 1 - bound * 2) + bound,
                                          random.nextInt(exec.getSize() + 1 - bound * 2) + bound);
                int numMiddlePoints = random.nextInt(maxMiddlePoints);
                float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
                exec.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError,
                          SymmetryType.TERRAIN);
            }
        });
    }

    @GraphMethod
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask pathInEdgeBounds(long seed, BooleanMask exec, float maxStepSize, int numPaths,
                                               int maxMiddlePoints, int bound, float maxAngleError) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            for (int i = 0; i < numPaths; i++) {
                int startX = random.nextInt(bound) + (random.nextBoolean() ? 0 : exec.getSize() - bound);
                int startY = random.nextInt(bound) + (random.nextBoolean() ? 0 : exec.getSize() - bound);
                int endX = random.nextInt(bound * 2) - bound + startX;
                int endY = random.nextInt(bound * 2) - bound + startY;
                Vector2 start = new Vector2(startX, startY);
                Vector2 end = new Vector2(endX, endY);
                int numMiddlePoints = random.nextInt(maxMiddlePoints);
                float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
                exec.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError,
                          SymmetryType.TERRAIN);
            }
        });
    }

    @GraphMethod
    @GraphParameter(name = "map", value = "map")
    @GraphParameter(name = "seed", value = "random.nextLong()")
    public static BooleanMask pathAroundSpawns(SCMap map, long seed, BooleanMask exec, float maxStepSize, int numPaths,
                                               int maxMiddlePoints, int bound, float maxAngleError) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            map.getSpawns().forEach(spawn -> {
                Vector2 start = new Vector2(spawn.getPosition());
                for (int i = 0; i < numPaths; i++) {
                    int endX = (int) (random.nextFloat() * bound + start.getX());
                    int endY = (int) (random.nextFloat() * bound + start.getY());
                    Vector2 end = new Vector2(endX, endY);
                    int numMiddlePoints = random.nextInt(maxMiddlePoints);
                    float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
                    exec.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError,
                              SymmetryType.TERRAIN);
                }
            });
        });
    }

    @GraphMethod
    @GraphParameter(name = "map", value = "map")
    public static BooleanMask fillSpawnCircle(SCMap map, BooleanMask exec, float radius) {
        return exec.enqueue(() -> map.getSpawns().forEach(spawn -> {
            Vector3 location = spawn.getPosition();
            exec.fillCircle(location, radius, true);
        }));
    }

    @GraphMethod
    @GraphParameter(name = "seed", value = "random.nextLong()")
    @GraphParameter(name = "map", value = "map")
    public static BooleanMask fillSpawnCircleWithProbability(SCMap map, long seed, BooleanMask exec, float spawnSize,
                                                             float probability) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            if (random.nextFloat() < probability) {
                map.getSpawns().forEach(spawn -> {
                    Vector3 location = spawn.getPosition();
                    exec.fillCircle(location, spawnSize, true);
                });
            }
        });
    }
}
