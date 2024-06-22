package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.mask.MapMaskMethods;

public class DropPlateauTerrainGenerator extends PathedTerrainGenerator {

    @Override
    protected void afterInitialize() {
        super.afterInitialize();
        plateauHeight = 12f;
        plateauBrushIntensity = 16f;
    }

    @Override
    protected void initRamps() {
        ramps.setSize(map.getSize() + 1);
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

        MapMaskMethods.connectTeamsAroundCenter(map, random.nextLong(), connections, minMiddlePoints, maxMiddlePoints,
                                                numTeamConnections, maxStepSize, 32);
        MapMaskMethods.connectTeammates(map, random.nextLong(), connections, maxMiddlePoints, numTeammateConnections,
                                        maxStepSize);
    }

    @Override
    protected void plateausSetup() {
        int mapSize = map.getSize();
        spawnPlateauMask.clear();
        plateaus.setSize(mapSize / 4);

        plateaus.randomWalk(
                (int) (plateauDensity * 4 / symmetrySettings.terrainSymmetry().getNumSymPoints() + 4),
                mapSize * 4);
        plateaus.dilute(.5f, 4);

        plateaus.setSize(mapSize + 1);
        plateaus.subtract(connections.copy().inflate(plateauBrushSize * 7f / 16f).blur(12, .125f));
    }
}


