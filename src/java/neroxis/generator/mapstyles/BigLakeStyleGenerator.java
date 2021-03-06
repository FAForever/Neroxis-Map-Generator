package neroxis.generator.mapstyles;

import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;

import java.util.Random;

public strictfp class BigLakeStyleGenerator extends DefaultStyleGenerator {

    public BigLakeStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        mountainBrushSize = 32;
        mountainBrushDensity = .05f;
        mountainBrushIntensity = 10;
        spawnSize = 48;
    }

    protected void landInit() {
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 8;
        int numWalkers = (int) (8 * (1 - landDensity) + 8) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = (int) (mapSize / 64 * (24 * (random.nextFloat() * .25f + (landDensity / MapStyle.BIG_LAKE.getLandDensityRange().getMax()) * .75f))) + mapSize / 8;
        land.setSize(mapSize + 1);
        land.invert();
        ConcurrentBinaryMask noLand = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "noLand");

        pathInCenterBounds(noLand, maxStepSize, numWalkers, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        noLand.inflate(1).setSize(mapSize / 4);
        noLand.grow(.5f, SymmetryType.TERRAIN, 10).setSize(mapSize + 1);
        noLand.smooth(mapSize / 64, .5f);
        land.minus(noLand);
    }

    protected void initRamps() {
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 2;
        int numPaths = (int) (rampDensity * 40) / symmetrySettings.getTerrainSymmetry().getNumSymPoints();
        int bound = mapSize / 4;
        ramps.setSize(mapSize + 1);

        pathInEdgeBounds(ramps, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));

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


