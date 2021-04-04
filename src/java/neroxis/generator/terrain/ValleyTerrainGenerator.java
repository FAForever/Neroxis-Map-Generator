package neroxis.generator.terrain;

import neroxis.generator.ParameterConstraints;
import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.SymmetrySettings;
import neroxis.map.SymmetryType;

public strictfp class ValleyTerrainGenerator extends PathedPlateauTerrainGenerator {

    public ValleyTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.75f, 1f)
                .mountainDensity(.5f, 1)
                .mapSizes(512, 1024)
                .build();
    }

    protected void landSetup() {
        land.setSize(map.getSize() + 1);
        land.invert();
    }

    protected void mountainSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedMountainDensity = parameterConstraints.getMountainDensityRange().normalize(mapParameters.getMountainDensity());
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

