package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.MapMaskMethods;

public strictfp class OneIslandTerrainGenerator extends PathedTerrainGenerator {

    public OneIslandTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .75f)
                .mapSizes(384, 1024)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters) {
        super.initialize(map, seed, generatorParameters);
        mountainBrushSize = 32;
        mountainBrushDensity = .1f;
        mountainBrushIntensity = 10;
    }

    @Override
    protected void landSetup() {
        SymmetrySettings symmetrySettings = generatorParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedLandDensity = parameterConstraints.getLandDensityRange().normalize(generatorParameters.getLandDensity());
        int minMiddlePoints = 2;
        int maxMiddlePoints = 4;
        int numTeamConnections = (int) (4 * normalizedLandDensity + 4) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int numTeammateConnections = (int) (2 * normalizedLandDensity + 2) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int numWalkers = (int) (8 * normalizedLandDensity + 8) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = (int) (mapSize / 64 * (16 * (random.nextFloat() * .25f + (1 - normalizedLandDensity) * .75f))) + mapSize / 8;
        float maxStepSize = mapSize / 128f;
        land.setSize(mapSize + 1);

        MapMaskMethods.pathInCenterBounds(random.nextLong(), land, maxStepSize, numWalkers, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        land.add(connections.copy().fillEdge((int) (mapSize / 8 * (1 - normalizedLandDensity) + mapSize / 8), false)
                .inflate(mapSize / 64f).blur(12, .125f));
        MapMaskMethods.connectTeamsAroundCenter(map, random.nextLong(), land, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize, 32);
        MapMaskMethods.connectTeammates(map, random.nextLong(), land, maxMiddlePoints, numTeammateConnections, maxStepSize);
        land.inflate(mapSize / 128f).setSize(mapSize / 8);
        land.dilute(.5f, 8).erode(.5f, 6);
        if (mapSize > 512) {
            land.erode(.5f, 4);
        }
        land.setSize(mapSize + 1);
        land.blur(mapSize / 64, .75f);
    }

    @Override
    protected void teamConnectionsSetup() {
        float maxStepSize = map.getSize() / 128f;
        int minMiddlePoints = 0;
        int maxMiddlePoints = 2;
        int numTeamConnections = (int) ((generatorParameters.getRampDensity() + generatorParameters.getPlateauDensity() + (1 - generatorParameters.getMountainDensity())) / 3 * 2 + 2);
        int numTeammateConnections = 1;
        connections.setSize(map.getSize() + 1);

        MapMaskMethods.connectTeams(map, random.nextLong(), connections, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize);
        MapMaskMethods.connectTeammates(map, random.nextLong(), connections, maxMiddlePoints, numTeammateConnections, maxStepSize);
    }
}


