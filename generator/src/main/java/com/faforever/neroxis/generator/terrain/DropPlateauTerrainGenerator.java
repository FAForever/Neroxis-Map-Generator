package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.MapMaskMethods;

public strictfp class DropPlateauTerrainGenerator extends PathedTerrainGenerator {

    public DropPlateauTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.5f, 1f)
                .plateauDensity(.5f, 1)
                .mexDensity(.25f, 1)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters, SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
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

        MapMaskMethods.connectTeamsAroundCenter(map, random.nextLong(), connections, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize, 32);
        MapMaskMethods.connectTeammates(map, random.nextLong(), connections, maxMiddlePoints, numTeammateConnections, maxStepSize);
    }

    @Override
    protected void plateausSetup() {
        int mapSize = map.getSize();
        float normalizedPlateauDensity = parameterConstraints.getPlateauDensityRange().normalize(generatorParameters.getPlateauDensity());
        spawnPlateauMask.clear();
        plateaus.setSize(mapSize / 4);

        plateaus.randomWalk((int) (normalizedPlateauDensity * 4 / symmetrySettings.getTerrainSymmetry().getNumSymPoints() + 4), mapSize * 4);
        plateaus.dilute(.5f, 4);

        plateaus.setSize(mapSize + 1);
        plateaus.subtract(connections.copy().inflate(plateauBrushSize * 7f / 16f).blur(12, .125f));
    }

    @Override
    protected void initRamps() {
        ramps.setSize(map.getSize() + 1);
    }
}


