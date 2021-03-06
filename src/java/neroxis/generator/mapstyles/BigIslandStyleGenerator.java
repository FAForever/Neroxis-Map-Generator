package neroxis.generator.mapstyles;

import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;

import java.util.Random;

public strictfp class BigIslandStyleGenerator extends DefaultStyleGenerator {

    public BigIslandStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        mountainBrushSize = 32;
        mountainBrushDensity = .1f;
        mountainBrushIntensity = 10;
        spawnSize = 48;
    }

    protected void landInit() {
        int minMiddlePoints = 2;
        int maxMiddlePoints = 4;
        int numTeamConnections = (int) (4 * landDensity + 4) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int numTeammateConnections = (int) (2 * landDensity + 2) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int numWalkers = (int) (8 * landDensity + 8) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = (int) (mapSize / 64 * (16 * (random.nextFloat() * .25f + (1 - landDensity / MapStyle.BIG_ISLAND.getLandDensityRange().getMax()) * .75f))) + mapSize / 8;
        float maxStepSize = mapSize / 128f;
        land.setSize(mapSize + 1);

        pathInCenterBounds(land, maxStepSize, numWalkers, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        land.combine(connections.copy().fillEdge((int) (mapSize / 8 * (1 - landDensity) + mapSize / 8), false)
                .inflate(mapSize / 64f).smooth(12, .125f));
        connectTeams(land, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize);
        connectTeammates(land, maxMiddlePoints, numTeammateConnections, maxStepSize);
        land.inflate(mapSize / 128f).setSize(mapSize / 8);
        land.grow(.5f, SymmetryType.SPAWN, 8).erode(.5f, SymmetryType.SPAWN, 6);
        if (mapSize > 512) {
            land.erode(.5f, SymmetryType.SPAWN, 3);
        }
        land.setSize(mapSize + 1);
        land.smooth(mapSize / 64, .75f);
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


