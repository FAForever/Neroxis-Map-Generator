package neroxis.generator.mapstyles;

import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;

import java.util.Random;

public strictfp abstract class PathedStyleGenerator extends DefaultStyleGenerator {

    public PathedStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
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


