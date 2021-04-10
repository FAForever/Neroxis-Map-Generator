package neroxis.generator.terrain;

import lombok.Getter;
import neroxis.generator.ElementGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;
import neroxis.util.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public abstract strictfp class TerrainGenerator extends ElementGenerator {
    protected ConcurrentFloatMask heightmap;
    protected ConcurrentBinaryMask impassable;
    protected ConcurrentBinaryMask unbuildable;
    protected ConcurrentBinaryMask passable;
    protected ConcurrentBinaryMask passableLand;
    protected ConcurrentBinaryMask passableWater;
    protected ConcurrentFloatMask slope;

    protected abstract void terrainSetup();

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        heightmap = new ConcurrentFloatMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "heightmapBase");
        slope = new ConcurrentFloatMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "slope");
        impassable = new ConcurrentBinaryMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "impassable");
        unbuildable = new ConcurrentBinaryMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "unbuildable");
        passable = new ConcurrentBinaryMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passable");
        passableLand = new ConcurrentBinaryMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passableLand");
        passableWater = new ConcurrentBinaryMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passableWater");
    }

    public void setHeightmapImage() {
        Pipeline.await(heightmap);
        Util.timedRun("neroxis.generator", "setHeightMap", () -> {
            map.setHeightImage(heightmap.getFinalMask());
            map.getHeightmap().getRaster().setPixel(0, 0, new int[]{0});
        });
    }

    @Override
    public void setupPipeline() {
        terrainSetup();
        passableSetup();
    }

    protected void passableSetup() {
        ConcurrentBinaryMask actualLand = new ConcurrentBinaryMask(heightmap, mapParameters.getBiome().getWaterSettings().getElevation(), random.nextLong(), "actualLand");

        slope.init(heightmap.copy().supcomGradient());
        impassable.init(slope, .7f);
        unbuildable.init(slope, .1f);

        impassable.inflate(4);

        passable.init(impassable).invert();
        passableLand.init(actualLand);
        passableWater.init(actualLand).invert();

        passable.fillEdge(8, false);
        passableLand.intersect(passable);
        passableWater.deflate(16).fillEdge(8, false);
    }

    protected void connectTeams(ConcurrentBinaryMask maskToUse, int minMiddlePoints, int maxMiddlePoints, int numConnections, float maxStepSize) {
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
            Vector2f start = new Vector2f(startSpawn.getPosition());
            Vector2f end = new Vector2f(start);
            float maxMiddleDistance = start.getDistance(end);
            maskToUse.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2, (float) (StrictMath.PI / 2), SymmetryType.SPAWN);
        }
    }

    protected void connectTeamsAroundCenter(ConcurrentBinaryMask maskToUse, int minMiddlePoints, int maxMiddlePoints, int numConnections, float maxStepSize) {
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
            Vector2f start = new Vector2f(startSpawn.getPosition());
            Vector2f end = new Vector2f(start);
            float offCenterAngle = (float) (StrictMath.PI * (1f / 4f + random.nextFloat() / 4f));
            offCenterAngle *= random.nextBoolean() ? 1 : -1;
            offCenterAngle += start.getAngle(new Vector2f(maskToUse.getPlannedSize() / 2f, maskToUse.getPlannedSize() / 2f));
            end.addPolar(offCenterAngle, random.nextFloat() * maskToUse.getPlannedSize() / 2f + maskToUse.getPlannedSize() / 2f);
            float maxMiddleDistance = start.getDistance(end);
            maskToUse.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2, (float) (StrictMath.PI / 2), SymmetryType.SPAWN);
        }
    }

    protected void connectTeammates(ConcurrentBinaryMask maskToUse, int maxMiddlePoints, int numConnections, float maxStepSize) {
        List<Spawn> startTeamSpawns = map.getSpawns().stream().filter(spawn -> spawn.getTeamID() == 0).collect(Collectors.toList());
        if (startTeamSpawns.size() > 1) {
            startTeamSpawns.forEach(startSpawn -> {
                for (int i = 0; i < numConnections; ++i) {
                    ArrayList<Spawn> otherSpawns = new ArrayList<>(startTeamSpawns);
                    otherSpawns.remove(startSpawn);
                    Spawn endSpawn = otherSpawns.get(random.nextInt(otherSpawns.size()));
                    int numMiddlePoints = random.nextInt(maxMiddlePoints);
                    Vector2f start = new Vector2f(startSpawn.getPosition());
                    Vector2f end = new Vector2f(endSpawn.getPosition());
                    float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
                    maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, (float) (StrictMath.PI / 2), SymmetryType.TERRAIN);
                }
            });
        }
    }

    protected void pathInCenterBounds(ConcurrentBinaryMask maskToUse, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        for (int i = 0; i < numPaths; i++) {
            Vector2f start = new Vector2f(random.nextInt(maskToUse.getPlannedSize() + 1 - bound * 2) + bound, random.nextInt(maskToUse.getPlannedSize() + 1 - bound * 2) + bound);
            Vector2f end = new Vector2f(random.nextInt(maskToUse.getPlannedSize() + 1 - bound * 2) + bound, random.nextInt(maskToUse.getPlannedSize() + 1 - bound * 2) + bound);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
    }

    protected void pathInEdgeBounds(ConcurrentBinaryMask maskToUse, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        for (int i = 0; i < numPaths; i++) {
            int startX = random.nextInt(bound) + (random.nextBoolean() ? 0 : maskToUse.getPlannedSize() - bound);
            int startY = random.nextInt(bound) + (random.nextBoolean() ? 0 : maskToUse.getPlannedSize() - bound);
            int endX = random.nextInt(bound * 2) - bound + startX;
            int endY = random.nextInt(bound * 2) - bound + startY;
            Vector2f start = new Vector2f(startX, startY);
            Vector2f end = new Vector2f(endX, endY);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
    }

    protected void pathAroundPoint(ConcurrentBinaryMask maskToUse, Vector2f start, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        for (int i = 0; i < numPaths; i++) {
            int endX = (int) (random.nextFloat() * bound + start.getX());
            int endY = (int) (random.nextFloat() * bound + start.getY());
            Vector2f end = new Vector2f(endX, endY);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
    }

}
