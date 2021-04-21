package neroxis.generator.terrain;

import neroxis.map.MapParameters;
import neroxis.map.SCMap;
import neroxis.map.SymmetryType;

public strictfp abstract class PathedTerrainGenerator extends BasicTerrainGenerator {

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
    }

    @Override
    protected void initRamps() {
        int mapSize = map.getSize();
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 2;
        int numPaths = (int) (mapParameters.getRampDensity() * 20) / mapParameters.getSymmetrySettings().getTerrainSymmetry().getNumSymPoints();
        int bound = mapSize / 4;
        ramps.setSize(mapSize + 1);

        pathInEdgeBounds(ramps, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        pathInCenterBounds(ramps, maxStepSize, numPaths / 2, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));

        ramps.minus(connections.copy().inflate(32)).inflate(maxStepSize / 2f).intersect(plateaus.copy().outline())
                .combine(connections.copy().inflate(maxStepSize / 2f).intersect(plateaus.copy().outline()))
                .minus(mountains).inflate(8);
    }

    @Override
    protected void spawnTerrainSetup() {
        int mapSize = map.getSize();
        spawnPlateauMask.setSize(mapSize / 4);
        spawnPlateauMask.erode(.5f, SymmetryType.SPAWN, 4).dilute(.5f, SymmetryType.SPAWN, 8);
        spawnPlateauMask.erode(.5f, SymmetryType.SPAWN).setSize(mapSize + 1);
        spawnPlateauMask.blur(4);

        spawnLandMask.setSize(mapSize / 4);
        spawnLandMask.erode(.25f, SymmetryType.SPAWN, mapSize / 128).dilute(.5f, SymmetryType.SPAWN, 4);
        spawnLandMask.erode(.5f, SymmetryType.SPAWN).setSize(mapSize + 1);
        spawnLandMask.blur(4);

        plateaus.minus(spawnLandMask).combine(spawnPlateauMask);
        land.combine(spawnLandMask).combine(spawnPlateauMask);

        ensureSpawnTerrain();

        mountains.intersect(land.copy().deflate(24));
    }
}


