package com.faforever.neroxis.mask;

import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapMaskMethods {
    private MapMaskMethods() {
    }

    public static BooleanMask connectTeams(List<Vector2> team0SpawnLocations, long seed, BooleanMask exec,
                                           int minMiddlePoints,
                                           int maxMiddlePoints, int numConnections, float maxStepSize) {
        Random random = new Random(seed);
        for (int i = 0; i < numConnections; ++i) {
            int numMiddlePoints;
            if (maxMiddlePoints > minMiddlePoints) {
                numMiddlePoints = random.nextInt(maxMiddlePoints - minMiddlePoints) + minMiddlePoints;
            } else {
                numMiddlePoints = maxMiddlePoints;
            }
            Vector2 start = team0SpawnLocations.get(random.nextInt(team0SpawnLocations.size())).copy();
            Vector2 end = start.copy();
            float maxMiddleDistance = start.getDistance(end);
            exec.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2,
                         (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
        }
        return exec;
    }

    public static BooleanMask connectTeamsAroundCenter(List<Vector2> team0SpawnLocations, long seed, BooleanMask exec,
                                                       int minMiddlePoints,
                                                       int maxMiddlePoints, int numConnections, float maxStepSize,
                                                       int bound) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            for (int i = 0; i < numConnections; ++i) {
                int numMiddlePoints;
                if (maxMiddlePoints > minMiddlePoints) {
                    numMiddlePoints = random.nextInt(maxMiddlePoints - minMiddlePoints) + minMiddlePoints;
                } else {
                    numMiddlePoints = maxMiddlePoints;
                }
                Vector2 start = team0SpawnLocations.get(random.nextInt(team0SpawnLocations.size())).copy();
                Vector2 end = start.copy();
                float offCenterAngle = (float) (StrictMath.PI * (1f / 3f + random.nextFloat() / 3f));
                offCenterAngle *= random.nextBoolean() ? 1 : -1;
                offCenterAngle += start.angleTo(new Vector2(exec.getSize() / 2f, exec.getSize() / 2f));
                end.addPolar(offCenterAngle, random.nextFloat() * exec.getSize() / 2f + exec.getSize() / 2f);
                end.clampMax(exec.getSize() - bound).clampMin(bound);
                float maxMiddleDistance = start.getDistance(end);
                exec.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2,
                             (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
            }
        });
    }

    public static BooleanMask connectTeammates(List<Vector2> team0SpawnLocations, long seed, BooleanMask exec,
                                               int maxMiddlePoints,
                                               int numConnections, float maxStepSize) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            if (team0SpawnLocations.size() > 1) {
                team0SpawnLocations.forEach(startSpawn -> {
                    for (int i = 0; i < numConnections; ++i) {
                        ArrayList<Vector2> otherSpawns = new ArrayList<>(team0SpawnLocations);
                        otherSpawns.remove(startSpawn);
                        Vector2 endSpawn = otherSpawns.get(random.nextInt(otherSpawns.size()));
                        int numMiddlePoints = random.nextInt(maxMiddlePoints);
                        Vector2 start = startSpawn.copy();
                        Vector2 end = endSpawn.copy();
                        float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
                        exec.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0,
                                  (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
                    }
                });
            }
        });
    }

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

    public static BooleanMask pathAroundSpawns(List<Vector2> team0SpawnLocations, long seed, BooleanMask exec,
                                               float maxStepSize, int numPaths,
                                               int maxMiddlePoints, int bound, float maxAngleError) {
        return exec.enqueue(() -> {
            Random random = new Random(seed);
            team0SpawnLocations.forEach(spawn -> {
                Vector2 start = spawn.copy();
                for (int i = 0; i < numPaths; i++) {
                    int endX = (int) (random.nextFloat() * bound + start.getX());
                    int endY = (int) (random.nextFloat() * bound + start.getY());
                    Vector2 end = new Vector2(endX, endY);
                    int numMiddlePoints = random.nextInt(maxMiddlePoints);
                    float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
                    exec.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError,
                              SymmetryType.SPAWN);
                }
            });
        });
    }
}
