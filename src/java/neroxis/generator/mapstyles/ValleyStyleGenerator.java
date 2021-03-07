package neroxis.generator.mapstyles;

import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;

import java.util.Random;

public strictfp class ValleyStyleGenerator extends PathedPlateauStyleGenerator {

    public ValleyStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
    }

    protected void landInit() {
        land.setSize(mapSize + 1);
        land.invert();
    }

    protected void mountainInit() {
        float normalizedMountainDensity = MapStyle.LITTLE_MOUNTAIN.getStyleConstraints().getMountainDensityRange().normalize(mountainDensity);
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 8;
        int numPaths = (int) (8 + 8 * (1 - normalizedMountainDensity) / symmetrySettings.getTerrainSymmetry().getNumSymPoints());
        int bound = (int) (mapSize / 16 * (3 * (random.nextFloat() * .25f + normalizedMountainDensity * .75f) + 1));
        mountains.setSize(mapSize + 1);
        ConcurrentBinaryMask noMountains = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "noMountains");

        pathInCenterBounds(noMountains, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        noMountains.setSize(mapSize / 4);
        noMountains.grow(.5f, SymmetryType.SPAWN, (int) (maxStepSize * 2)).setSize(mapSize + 1);
        noMountains.smooth(mapSize / 64);

        mountains.invert().minus(noMountains);
    }
}

