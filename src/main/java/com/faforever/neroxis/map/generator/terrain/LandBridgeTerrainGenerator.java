package com.faforever.neroxis.map.generator.terrain;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.util.Vector2;

public strictfp class LandBridgeTerrainGenerator extends PathedTerrainGenerator {

    public LandBridgeTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.25f, .75f)
                .mexDensity(.5f, 1f)
                .mapSizes(1024)
                .numTeams(2, 4)
                .build();
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();
        float maxStepSize = mapSize / 128f;
        int numPaths = 32 / mapParameters.getSpawnCount();

        land.setSize(mapSize + 1);
        connectTeammates(land, 8, 2, maxStepSize);
        connectTeams(land, 0, 2, 1, maxStepSize);
        map.getSpawns().forEach(spawn ->
                pathAroundPoint(land, new Vector2(spawn.getPosition()), maxStepSize, numPaths, 4, mapSize / 6, (float) (StrictMath.PI / 2f)));
        land.inflate(maxStepSize);
        land.setSize(mapSize / 8);
        land.dilute(.5f, 8);
        land.setSize(mapSize + 1);
        land.blur(8);
    }

    @Override
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
        plateaus.dilute(.5f, 4).setSize(mapSize + 1);
        plateaus.blur(12);
    }
}

