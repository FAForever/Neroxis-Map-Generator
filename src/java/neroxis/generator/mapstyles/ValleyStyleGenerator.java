package neroxis.generator.mapstyles;

import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;

import java.util.Random;

public strictfp class ValleyStyleGenerator extends DefaultStyleGenerator {

    public ValleyStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
    }

    protected void landInit() {
        land.setSize(mapSize + 1);
        land.invert();
    }

    protected void plateausInit() {
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 16;
        int numPaths = (int) (12 * plateauDensity) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = 0;
        plateaus.setSize(mapSize + 1);

        pathInCenterBounds(plateaus, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        plateaus.inflate(mapSize / 256f).setSize(mapSize / 4);
        plateaus.grow(.5f, SymmetryType.TERRAIN, 4).setSize(mapSize + 1);
        plateaus.smooth(12);
    }

    protected void mountainInit() {
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 8;
        int numPaths = (int) (8 + 8 * (1 - mountainDensity) / symmetrySettings.getTerrainSymmetry().getNumSymPoints());
        int bound = (int) (mapSize / 16 * (3 * (random.nextFloat() * .25f + mountainDensity * .75f) + 1));
        mountains.setSize(mapSize + 1);
        ConcurrentBinaryMask noMountains = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "noMountains");

        pathInCenterBounds(noMountains, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        noMountains.setSize(mapSize / 4);
        noMountains.grow(.5f, SymmetryType.SPAWN, (int) (maxStepSize * 2)).setSize(mapSize + 1);
        noMountains.smooth(mapSize / 64);

        mountains.invert().minus(noMountains);
    }
}

