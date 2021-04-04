package neroxis.generator.terrain;

import neroxis.generator.ParameterConstraints;
import neroxis.map.SymmetrySettings;
import neroxis.map.SymmetryType;
import neroxis.util.Vector2f;

public strictfp class LandBridgeTerrainGenerator extends PathedTerrainGenerator {

    public LandBridgeTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.25f, .75f)
                .mexDensity(.5f, 1f)
                .mapSizes(1024)
                .build();
    }

    protected void landSetup() {
        int mapSize = map.getSize();
        float maxStepSize = mapSize / 128f;
        int numPaths = 32 / mapParameters.getSpawnCount();

        land.setSize(mapSize + 1);
        connectTeammates(land, 8, 2, maxStepSize);
        connectTeams(land, 0, 2, 1, maxStepSize);
        map.getSpawns().forEach(spawn -> {
            pathAroundPoint(land, new Vector2f(spawn.getPosition()), maxStepSize, numPaths, 4, mapSize / 6, (float) (StrictMath.PI / 2f));
        });
        land.inflate(maxStepSize);
        land.setSize(mapSize / 8);
        land.grow(.5f, SymmetryType.SPAWN, 8);
        land.setSize(mapSize + 1);
        land.smooth(8);
    }

    protected void plateausSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 2;
        int numPaths = (int) (16 * mapParameters.getPlateauDensity()) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = mapSize / 4;
        plateaus.setSize(mapSize + 1);

        pathInEdgeBounds(plateaus, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        plateaus.inflate(mapSize / 256f).setSize(mapSize / 4);
        plateaus.grow(.5f, SymmetryType.TERRAIN, 4).setSize(mapSize + 1);
        plateaus.smooth(12);
    }
}

