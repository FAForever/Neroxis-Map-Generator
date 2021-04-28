package com.faforever.neroxis.map.generator.terrain;

import com.faforever.neroxis.map.*;
import com.faforever.neroxis.map.generator.ElementGenerator;
import com.faforever.neroxis.map.mask.BooleanMask;
import com.faforever.neroxis.map.mask.FloatMask;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.Util;
import com.faforever.neroxis.util.Vector2;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public abstract strictfp class TerrainGenerator extends ElementGenerator {
    protected FloatMask heightmap;
    protected BooleanMask impassable;
    protected BooleanMask unbuildable;
    protected BooleanMask passable;
    protected BooleanMask passableLand;
    protected BooleanMask passableWater;
    protected FloatMask slope;

    protected abstract void terrainSetup();

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        heightmap = new FloatMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "heightmap", true);
        slope = new FloatMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "slope", true);
        impassable = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "impassable", true);
        unbuildable = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "unbuildable", true);
        passable = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passable", true);
        passableLand = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passableLand", true);
        passableWater = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passableWater", true);
    }

    public void setHeightmapImage() {
        Pipeline.await(heightmap);
        Util.timedRun("com.faforever.neroxis.map.generator", "setHeightMap", () -> {
            heightmap.getFinalMask().writeToImage(map.getHeightmap(), 1 / map.getHeightMapScale());
            map.getHeightmap().getRaster().setPixel(0, 0, new int[]{0});
        });
    }

    @Override
    public void setupPipeline() {
        terrainSetup();
        passableSetup();
    }

    protected void passableSetup() {
        BooleanMask actualLand = new BooleanMask(heightmap, mapParameters.getBiome().getWaterSettings().getElevation(), random.nextLong(), "actualLand");

        slope.init(heightmap.copy().supcomGradient());
        impassable.init(slope, .7f);
        unbuildable.init(slope, .1f);

        impassable.inflate(4);

        passable.init(impassable).invert();
        passableLand.init(actualLand);
        passableWater.init(actualLand).invert();

        passable.fillEdge(8, false);
        passableLand.multiply(passable);
        passableWater.deflate(16).fillEdge(8, false);
    }

    protected void connectTeams(BooleanMask maskToUse, int minMiddlePoints, int maxMiddlePoints, int numConnections, float maxStepSize) {
        if (mapParameters.getNumTeams() == 0) {
            return;
        }
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
            maskToUse.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2, (float) (StrictMath.PI / 2), SymmetryType.SPAWN);
        }
    }

    protected void connectTeamsAroundCenter(BooleanMask maskToUse, int minMiddlePoints, int maxMiddlePoints, int numConnections, float maxStepSize,
                                            int bound) {
        if (mapParameters.getNumTeams() == 0) {
            return;
        }
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
            float offCenterAngle = (float) (StrictMath.PI * (1f / 4f + random.nextFloat() / 4f));
            offCenterAngle *= random.nextBoolean() ? 1 : -1;
            offCenterAngle += start.angleTo(new Vector2(maskToUse.getSize() / 2f, maskToUse.getSize() / 2f));
            end.addPolar(offCenterAngle, random.nextFloat() * maskToUse.getSize() / 2f + maskToUse.getSize() / 2f);
            end.clampMax(maskToUse.getSize() - bound).clampMin(bound);
            float maxMiddleDistance = start.getDistance(end);
            maskToUse.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2, (float) (StrictMath.PI / 2), SymmetryType.SPAWN);
        }
    }

    protected void connectTeammates(BooleanMask maskToUse, int maxMiddlePoints, int numConnections, float maxStepSize) {
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
                    maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
                }
            });
        }
    }

    protected void pathInCenterBounds(BooleanMask maskToUse, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        for (int i = 0; i < numPaths; i++) {
            Vector2 start = new Vector2(random.nextInt(maskToUse.getSize() + 1 - bound * 2) + bound, random.nextInt(maskToUse.getSize() + 1 - bound * 2) + bound);
            Vector2 end = new Vector2(random.nextInt(maskToUse.getSize() + 1 - bound * 2) + bound, random.nextInt(maskToUse.getSize() + 1 - bound * 2) + bound);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
    }

    protected void pathInEdgeBounds(BooleanMask maskToUse, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        for (int i = 0; i < numPaths; i++) {
            int startX = random.nextInt(bound) + (random.nextBoolean() ? 0 : maskToUse.getSize() - bound);
            int startY = random.nextInt(bound) + (random.nextBoolean() ? 0 : maskToUse.getSize() - bound);
            int endX = random.nextInt(bound * 2) - bound + startX;
            int endY = random.nextInt(bound * 2) - bound + startY;
            Vector2 start = new Vector2(startX, startY);
            Vector2 end = new Vector2(endX, endY);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
    }

    protected void pathAroundPoint(BooleanMask maskToUse, Vector2 start, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        for (int i = 0; i < numPaths; i++) {
            int endX = (int) (random.nextFloat() * bound + start.getX());
            int endY = (int) (random.nextFloat() * bound + start.getY());
            Vector2 end = new Vector2(endX, endY);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
    }

}
