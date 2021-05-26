package com.faforever.neroxis.map.generator.terrain;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.generator.ParameterConstraints;

public strictfp class DropPlateauTerrainGenerator extends PathedTerrainGenerator {

    public DropPlateauTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.5f, 1f)
                .plateauDensity(.5f, 1)
                .mexDensity(.25f, 1)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        plateauHeight = 12f;
        plateauBrushIntensity = 16f;
    }

    @Override
    protected void teamConnectionsSetup() {
        int mapSize = map.getSize();
        float maxStepSize = mapSize / 128f;
        int minMiddlePoints = 0;
        int maxMiddlePoints = 2;
        int numTeamConnections = 2;
        int numTeammateConnections = 1;

        connections.setSize(mapSize + 1);

        connectTeamsAroundCenter(connections, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize, 32);
        connectTeammates(connections, maxMiddlePoints, numTeammateConnections, maxStepSize);
    }

    @Override
    protected void plateausSetup() {
        int mapSize = map.getSize();
        float normalizedPlateauDensity = parameterConstraints.getPlateauDensityRange().normalize(mapParameters.getPlateauDensity());
        spawnPlateauMask.clear();
        plateaus.setSize(mapSize / 4);

        plateaus.randomWalk((int) (normalizedPlateauDensity * 4 / mapParameters.getSymmetrySettings().getTerrainSymmetry().getNumSymPoints() + 4), mapSize * 4);
        plateaus.dilute(.5f, 4);

        plateaus.setSize(mapSize + 1);
        plateaus.subtract(connections.copy().inflate(plateauBrushSize * 7f / 16f).blur(12, .125f));
    }

    @Override
    protected void initRamps() {
        ramps.setSize(map.getSize() + 1);
    }
}


