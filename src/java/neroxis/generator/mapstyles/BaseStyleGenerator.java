package neroxis.generator.mapstyles;

import lombok.Getter;
import lombok.Setter;
import neroxis.biomes.Biome;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Getter
@Setter
public strictfp abstract class BaseStyleGenerator {

    public static boolean DEBUG = false;

    protected final MapParameters mapParameters;
    protected final SCMap map;
    protected final int spawnCount;
    protected final float landDensity;
    protected final float plateauDensity;
    protected final float mountainDensity;
    protected final float rampDensity;
    protected final int mapSize;
    protected final int numTeams;
    protected final float reclaimDensity;
    protected final int mexCount;
    protected final int hydroCount;
    protected final SymmetrySettings symmetrySettings;
    protected final Biome biome;
    protected final boolean unexplored;
    //masks used in generation
    protected final ConcurrentBinaryMask land;
    protected final ConcurrentBinaryMask mountains;
    protected final ConcurrentBinaryMask hills;
    protected final ConcurrentBinaryMask valleys;
    protected final ConcurrentBinaryMask plateaus;
    protected final ConcurrentBinaryMask ramps;
    protected final ConcurrentBinaryMask connections;
    protected final ConcurrentBinaryMask impassable;
    protected final ConcurrentBinaryMask unbuildable;
    protected final ConcurrentBinaryMask notFlat;
    protected final ConcurrentBinaryMask passable;
    protected final ConcurrentBinaryMask passableLand;
    protected final ConcurrentBinaryMask passableWater;
    protected final ConcurrentFloatMask slope;
    protected final ConcurrentFloatMask heightmapBase;
    protected final ConcurrentBinaryMask spawnLandMask;
    protected final ConcurrentBinaryMask spawnPlateauMask;
    protected final ConcurrentBinaryMask fieldDecal;
    protected final ConcurrentBinaryMask slopeDecal;
    protected final ConcurrentBinaryMask mountainDecal;
    protected final ConcurrentBinaryMask resourceMask;
    protected final ConcurrentBinaryMask waterResourceMask;
    protected final ConcurrentFloatMask accentGroundTexture;
    protected final ConcurrentFloatMask waterBeachTexture;
    protected final ConcurrentFloatMask accentSlopesTexture;
    protected final ConcurrentFloatMask accentPlateauTexture;
    protected final ConcurrentFloatMask slopesTexture;
    protected final ConcurrentFloatMask steepHillsTexture;
    protected final ConcurrentFloatMask rockTexture;
    protected final ConcurrentFloatMask accentRockTexture;
    protected Random random;
    protected boolean generationComplete;
    protected float waterHeight;
    protected boolean hasCivilians;
    protected boolean enemyCivilians;
    protected float plateauHeight = 6f;
    protected float oceanFloor = -16f;
    protected float valleyFloor = -5f;
    protected float landHeight = .05f;
    protected int mountainBrushSize = 64;
    protected int plateauBrushSize = 32;
    protected int smallFeatureBrushSize = 24;
    protected int spawnSize = 36;

    public BaseStyleGenerator(MapParameters mapParameters, Random random) {
        this.mapParameters = mapParameters;
        this.random = random;
        landDensity = mapParameters.getLandDensity();
        mountainDensity = mapParameters.getMountainDensity();
        plateauDensity = mapParameters.getPlateauDensity();
        rampDensity = mapParameters.getRampDensity();
        reclaimDensity = mapParameters.getReclaimDensity();
        mexCount = mapParameters.getMexCount();
        hydroCount = mapParameters.getHydroCount();
        spawnCount = mapParameters.getSpawnCount();
        mapSize = mapParameters.getMapSize();
        numTeams = mapParameters.getNumTeams();
        symmetrySettings = mapParameters.getSymmetrySettings();
        unexplored = mapParameters.isUnexplored();
        biome = mapParameters.getBiome();
        waterHeight = biome.getWaterSettings().getElevation();
        if (mapSize < 512) {
            mountainBrushSize = 32;
        }
        map = new SCMap(mapSize, spawnCount, mexCount, hydroCount, biome);

        Pipeline.reset();

        land = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "land");
        mountains = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "mountains");
        plateaus = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "plateaus");
        ramps = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "ramps");
        hills = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "hills");
        valleys = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "valleys");
        connections = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "connections");
        impassable = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "impassable");
        unbuildable = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "unbuildable");
        notFlat = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "notFlat");
        passable = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "passable");
        passableLand = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "passableLand");
        passableWater = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "passableWater");
        spawnLandMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "land");
        spawnPlateauMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "spawnPlateauMask");
        resourceMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "resourceMask");
        waterResourceMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "waterResourceMask");
        fieldDecal = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "fieldDecal");
        slopeDecal = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "slopeDecal");
        mountainDecal = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "mountainDecal");
        accentGroundTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentGroundTexture");
        waterBeachTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "waterBeachTexture");
        accentSlopesTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentSlopesTexture");
        accentPlateauTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentPlateauTexture");
        slopesTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "slopesTexture");
        steepHillsTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "steepHillsTexture");
        rockTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "rockTexture");
        accentRockTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentRockTexture");
        slope = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "slope");
        heightmapBase = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "heightmapBase");
    }

    public abstract SCMap generate();

    protected abstract void setupPipeline();

    protected void connectTeams(ConcurrentBinaryMask maskToUse, int minMiddlePoints, int maxMiddlePoints, int numConnections, float maxStepSize) {
        if (numTeams == 0) {
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
            float offCenterAngle = (float) (StrictMath.PI * (1f / 3f + random.nextFloat() / 3f));
            offCenterAngle *= random.nextBoolean() ? 1 : -1;
            offCenterAngle += start.getAngle(new Vector2f(mapSize / 2f, mapSize / 2f));
            end.addPolar(offCenterAngle, random.nextFloat() * mapSize / 4f + mapSize / 4f);
            float maxMiddleDistance = start.getDistance(end);
            maskToUse.connect(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, maxMiddleDistance / 2, (float) (StrictMath.PI / 2), SymmetryType.SPAWN);
        }
    }

    protected void connectTeammates(ConcurrentBinaryMask maskToUse, int maxMiddlePoints, int numConnections, float maxStepSize) {
        if (numTeams == 0) {
            return;
        }
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
            Vector2f start = new Vector2f(random.nextInt(mapSize + 1 - bound * 2) + bound, random.nextInt(mapSize + 1 - bound * 2) + bound);
            Vector2f end = new Vector2f(random.nextInt(mapSize + 1 - bound * 2) + bound, random.nextInt(mapSize + 1 - bound * 2) + bound);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
    }

    protected void pathInEdgeBounds(ConcurrentBinaryMask maskToUse, float maxStepSize, int numPaths, int maxMiddlePoints, int bound, float maxAngleError) {
        for (int i = 0; i < numPaths; i++) {
            int startX = random.nextInt(bound) + (random.nextBoolean() ? 0 : mapSize - bound);
            int startY = random.nextInt(bound) + (random.nextBoolean() ? 0 : mapSize - bound);
            int endX = random.nextInt(bound * 2) - bound + startX;
            int endY = random.nextInt(bound * 2) - bound + startY;
            Vector2f start = new Vector2f(startX, startY);
            Vector2f end = new Vector2f(endX, endY);
            int numMiddlePoints = random.nextInt(maxMiddlePoints);
            float maxMiddleDistance = start.getDistance(end) / numMiddlePoints * 2;
            maskToUse.path(start, end, maxStepSize, numMiddlePoints, maxMiddleDistance, 0, maxAngleError, SymmetryType.TERRAIN);
        }
    }

    protected void generateExclusionZones(BinaryMask mask, float spawnSpacing, float mexSpacing, float hydroSpacing) {
        map.getSpawns().forEach(spawn -> mask.fillCircle(spawn.getPosition(), spawnSpacing, true));
        map.getMexes().forEach(mex -> mask.fillCircle(mex.getPosition(), mexSpacing, true));
        map.getHydros().forEach(hydro -> mask.fillCircle(hydro.getPosition(), hydroSpacing, true));
    }

    protected void setupDecalPipeline() {
        fieldDecal.init(land);
        slopeDecal.init(slope, .25f);
        mountainDecal.init(mountains);

        fieldDecal.minus(slopeDecal.copy().inflate(16)).minus(mountainDecal);
    }

    protected void setupResourcePipeline() {
        resourceMask.init(land);
        waterResourceMask.init(land).invert();

        resourceMask.minus(unbuildable).deflate(4);
        resourceMask.fillEdge(16, false).fillCenter(24, false);
        waterResourceMask.minus(unbuildable).deflate(8).fillEdge(16, false).fillCenter(24, false);
    }

    protected void setupTexturePipeline() {
        ConcurrentBinaryMask flat = new ConcurrentBinaryMask(slope, .05f, random.nextLong(), "flat").invert();
        ConcurrentBinaryMask highGround = new ConcurrentBinaryMask(heightmapBase, waterHeight + plateauHeight * 3 / 4f, random.nextLong(), "highGround");
        ConcurrentBinaryMask accentGround = new ConcurrentBinaryMask(land, random.nextLong(), "accentGround");
        ConcurrentBinaryMask accentPlateau = new ConcurrentBinaryMask(plateaus, random.nextLong(), "accentPlateau");
        ConcurrentBinaryMask slopes = new ConcurrentBinaryMask(slope, .15f, random.nextLong(), "slopes");
        ConcurrentBinaryMask accentSlopes = new ConcurrentBinaryMask(slope, .55f, random.nextLong(), "accentSlopes").invert();
        ConcurrentBinaryMask steepHills = new ConcurrentBinaryMask(slope, .55f, random.nextLong(), "steepHills");
        ConcurrentBinaryMask rock = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "rock");
        ConcurrentBinaryMask accentRock = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "accentRock");

        accentGround.minus(highGround).acid(.1f, 0).erode(.4f, SymmetryType.SPAWN).smooth(6, .75f);
        accentPlateau.acid(.1f, 0).erode(.4f, SymmetryType.SPAWN).smooth(6, .75f);
        slopes.flipValues(.95f).erode(.5f, SymmetryType.SPAWN).acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentSlopes.minus(flat).acid(.1f, 0).erode(.5f, SymmetryType.SPAWN).smooth(4, .75f).acid(.55f, 0);
        steepHills.acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentRock.acid(.2f, 0).erode(.3f, SymmetryType.SPAWN).acid(.2f, 0).smooth(2, .5f).intersect(rock);

        accentGroundTexture.init(accentGround, 0, .5f).smooth(12).add(accentGround, .325f).smooth(8).add(accentGround, .25f).clampMax(1f).smooth(2);
        accentPlateauTexture.init(accentPlateau, 0, .5f).smooth(12).add(accentPlateau, .325f).smooth(8).add(accentPlateau, .25f).clampMax(1f).smooth(2);
        slopesTexture.init(slopes, 0, 1).smooth(8).add(slopes, .75f).smooth(4).clampMax(1f);
        accentSlopesTexture.init(accentSlopes, 0, 1).smooth(8).add(accentSlopes, .65f).smooth(4).add(accentSlopes, .5f).smooth(1).clampMax(1f);
        steepHillsTexture.init(steepHills, 0, 1).smooth(8).clampMax(0.35f).add(steepHills, .65f).smooth(4).clampMax(0.65f).add(steepHills, .5f).smooth(1).clampMax(1f);
        waterBeachTexture.init(land.copy().invert().inflate(12).minus(plateaus.copy().minus(ramps)), 0, 1).smooth(12);
        rockTexture.init(rock, 0, 1f).smooth(4).add(rock, 1f).smooth(2).clampMax(1f);
        accentRockTexture.init(accentRock, 0, 1f).smooth(4).clampMax(1f);
    }
}

