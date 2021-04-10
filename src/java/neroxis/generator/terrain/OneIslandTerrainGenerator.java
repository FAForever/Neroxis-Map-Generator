package neroxis.generator.terrain;

import neroxis.generator.ParameterConstraints;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;
import neroxis.map.SymmetrySettings;
import neroxis.map.SymmetryType;

public strictfp class OneIslandTerrainGenerator extends PathedTerrainGenerator {

    public OneIslandTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .75f)
                .mapSizes(512, 1024)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        mountainBrushSize = 32;
        mountainBrushDensity = .1f;
        mountainBrushIntensity = 10;
    }

    @Override
    protected void landSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedLandDensity = parameterConstraints.getLandDensityRange().normalize(mapParameters.getLandDensity());
        int minMiddlePoints = 2;
        int maxMiddlePoints = 4;
        int numTeamConnections = (int) (4 * normalizedLandDensity + 4) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int numTeammateConnections = (int) (2 * normalizedLandDensity + 2) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int numWalkers = (int) (8 * normalizedLandDensity + 8) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = (int) (mapSize / 64 * (16 * (random.nextFloat() * .25f + (1 - normalizedLandDensity) * .75f))) + mapSize / 8;
        float maxStepSize = mapSize / 128f;
        land.startVisualDebugger();
        connections.startVisualDebugger();
        land.setSize(mapSize + 1);

        pathInCenterBounds(land, maxStepSize, numWalkers, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        land.combine(connections.copy().fillEdge((int) (mapSize / 8 * (1 - normalizedLandDensity) + mapSize / 8), false)
                .inflate(mapSize / 64f).smooth(12, .125f));
        connectTeamsAroundCenter(land, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize);
        connectTeammates(land, maxMiddlePoints, numTeammateConnections, maxStepSize);
        land.inflate(mapSize / 128f).setSize(mapSize / 8);
        land.grow(.5f, SymmetryType.SPAWN, 8).erode(.5f, SymmetryType.SPAWN, 6);
        if (mapSize > 512) {
            land.erode(.5f, SymmetryType.SPAWN, 4);
        }
        land.setSize(mapSize + 1);
        land.smooth(mapSize / 64, .75f);
    }

    @Override
    protected void teamConnectionsSetup() {
        float maxStepSize = map.getSize() / 128f;
        int minMiddlePoints = 0;
        int maxMiddlePoints = 2;
        int numTeamConnections = (int) ((mapParameters.getRampDensity() + mapParameters.getPlateauDensity() + (1 - mapParameters.getMountainDensity())) / 3 * 2 + 1);
        int numTeammateConnections = 1;
        connections.setSize(map.getSize() + 1);

        connectTeams(connections, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize);
        connectTeammates(connections, maxMiddlePoints, numTeammateConnections, maxStepSize);
    }
}


