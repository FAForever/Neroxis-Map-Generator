package neroxis.generator.terrain;

import neroxis.generator.ParameterConstraints;
import neroxis.map.*;
import neroxis.util.Vector2f;

public strictfp class SmallIslandsTerrainGenerator extends PathedTerrainGenerator {

    public SmallIslandsTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .5f)
                .plateauDensity(0, .5f)
                .mexDensity(.5f, 1)
                .mapSizes(1024)
                .build();
    }

    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        spawnSize = 64;
    }

    protected void landSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedLandDensity = parameterConstraints.getLandDensityRange().normalize(mapParameters.getLandDensity());
        int maxMiddlePoints = 4;
        int numPaths = (int) (4 * normalizedLandDensity + 4) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = ((int) (mapSize / 16 * (random.nextFloat() * .25f + normalizedLandDensity * .75f)) + mapSize / 16);
        float maxStepSize = mapSize / 128f;

        ConcurrentBinaryMask islands = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "islands");

        land.setSize(mapSize + 1);
        map.getSpawns().forEach(spawn -> pathAroundPoint(land, new Vector2f(spawn.getPosition()), maxStepSize, numPaths, maxMiddlePoints, bound, (float) StrictMath.PI / 2));
        land.inflate(maxStepSize).setSize(mapSize / 4);

        islands.randomWalk((int) (normalizedLandDensity * 40 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()) + 2, mapSize / 8);

        land.combine(islands);
        land.grow(.5f, SymmetryType.SPAWN, 8);

        land.setSize(mapSize + 1);
        land.smooth(16);
    }
}


