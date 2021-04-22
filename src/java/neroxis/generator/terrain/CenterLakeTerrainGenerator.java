package neroxis.generator.terrain;

import neroxis.generator.ParameterConstraints;
import neroxis.map.*;

public strictfp class CenterLakeTerrainGenerator extends PathedTerrainGenerator {

    public CenterLakeTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .5f)
                .plateauDensity(0, .5f)
                .mexDensity(.25f, 1)
                .mapSizes(512, 1024)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        mountainBrushSize = 32;
        mountainBrushDensity = .05f;
        mountainBrushIntensity = 10;
    }

    @Override
    protected void landSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedLandDensity = parameterConstraints.getLandDensityRange().normalize(mapParameters.getLandDensity());
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 8;
        int numWalkers = (int) (8 * (1 - normalizedLandDensity) + 8) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = (int) (mapSize / 64 * (24 * (random.nextFloat() * .25f + normalizedLandDensity * .75f))) + mapSize / 8;
        land.setSize(mapSize + 1);
        land.invert();
        BooleanMask noLand = new BooleanMask(mapSize + 1, random.nextLong(), symmetrySettings, "noLand", true);

        pathInCenterBounds(noLand, maxStepSize, numWalkers, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        noLand.inflate(1).setSize(mapSize / 4);
        noLand.dilute(.5f, SymmetryType.TERRAIN, 10).setSize(mapSize + 1);
        noLand.blur(mapSize / 64, .5f);
        land.minus(noLand);
    }
}


