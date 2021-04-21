package neroxis.generator.terrain;

import neroxis.generator.ParameterConstraints;
import neroxis.map.BinaryMask;
import neroxis.map.SymmetrySettings;
import neroxis.map.SymmetryType;
import neroxis.util.Vector2f;

public strictfp class BigIslandsTerrainGenerator extends PathedTerrainGenerator {

    public BigIslandsTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .75f)
                .plateauDensity(0, .5f)
                .mapSizes(1024)
                .build();
        weight = 2;
    }

    @Override
    protected void landSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedLandDensity = parameterConstraints.getLandDensityRange().normalize(mapParameters.getLandDensity());
        int maxMiddlePoints = 4;
        int numPaths = (int) (8 * normalizedLandDensity + 8) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = ((int) (mapSize / 8 * (random.nextFloat() * .25f + normalizedLandDensity * .75f)) + mapSize / 8);
        float maxStepSize = mapSize / 128f;

        BinaryMask islands = new BinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "islands", true);

        land.setSize(mapSize + 1);
        map.getSpawns().forEach(spawn -> pathAroundPoint(land, new Vector2f(spawn.getPosition()), maxStepSize, numPaths, maxMiddlePoints, bound, (float) StrictMath.PI / 2));
        land.inflate(maxStepSize).setSize(mapSize / 4);

        islands.randomWalk((int) (normalizedLandDensity * 20 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()) + 2, mapSize * 4);
        islands.minus(land.copy().inflate(32));

        land.combine(islands);
        land.dilute(.5f, SymmetryType.SPAWN, 8);

        land.setSize(mapSize + 1);
        land.blur(16);
    }
}


