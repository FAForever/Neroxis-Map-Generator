package neroxis.generator.mapstyles;

import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;
import neroxis.util.Vector2f;

import java.util.Random;

public strictfp class IslandsStyleGenerator extends DefaultStyleGenerator {

    public IslandsStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        spawnSize = 64;
    }

    protected void landInit() {
        int maxMiddlePoints = 4;
        int numPaths = (int) (8 * landDensity + 8) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = ((int) (mapSize / 8 * (random.nextFloat() * .25f + landDensity * .75f)) + mapSize / 8);
        float maxStepSize = mapSize / 128f;

        ConcurrentBinaryMask islands = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "islands");
        islands.startVisualDebugger();

        land.startVisualDebugger();

        land.setSize(mapSize + 1);
        map.getSpawns().forEach(spawn -> pathAroundPoint(land, new Vector2f(spawn.getPosition()), maxStepSize, maxMiddlePoints, numPaths, bound, (float) StrictMath.PI / 2));
        land.inflate(maxStepSize).setSize(mapSize / 4);

        islands.randomWalk((int) (landDensity * 25 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()), mapSize / 4);
        islands.minus(land.copy().inflate(mapSize / 128f));

        land.combine(islands);
        land.grow(.5f, SymmetryType.SPAWN, 8);

        land.setSize(mapSize + 1);
        land.smooth(16);
    }

    protected void initRamps() {
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 2;
        int numPaths = (int) (rampDensity * 20) / symmetrySettings.getTerrainSymmetry().getNumSymPoints();
        int bound = mapSize / 4;
        ramps.setSize(mapSize + 1);

        pathInEdgeBounds(ramps, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        pathInCenterBounds(ramps, maxStepSize, numPaths / 2, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));

        ramps.minus(connections.copy().inflate(32)).inflate(maxStepSize / 2f).intersect(plateaus.copy().outline())
                .space(6, 12).combine(connections.copy().inflate(maxStepSize / 2f).intersect(plateaus.copy().outline()))
                .inflate(24);
    }

    protected void addSpawnTerrain() {
        spawnPlateauMask.setSize(mapSize / 4);
        spawnPlateauMask.erode(.5f, SymmetryType.SPAWN, 4).grow(.5f, SymmetryType.SPAWN, 8);
        spawnPlateauMask.erode(.5f, SymmetryType.SPAWN).setSize(mapSize + 1);
        spawnPlateauMask.smooth(4);

        spawnLandMask.setSize(mapSize / 4);
        spawnLandMask.erode(.25f, SymmetryType.SPAWN, mapSize / 128).grow(.5f, SymmetryType.SPAWN, 4);
        spawnLandMask.erode(.5f, SymmetryType.SPAWN).setSize(mapSize + 1);
        spawnLandMask.smooth(4);

        plateaus.minus(spawnLandMask).combine(spawnPlateauMask);
        land.combine(spawnLandMask).combine(spawnPlateauMask);

        mountains.minus(connections.copy().inflate(mountainBrushSize / 2f).smooth(12, .125f));
        mountains.minus(spawnLandMask.copy().inflate(mountainBrushSize / 2f));

        plateaus.intersect(land).minus(spawnLandMask).combine(spawnPlateauMask);
        land.combine(plateaus).combine(spawnLandMask).combine(spawnPlateauMask);

        mountains.intersect(land.copy().deflate(24));
    }
}


