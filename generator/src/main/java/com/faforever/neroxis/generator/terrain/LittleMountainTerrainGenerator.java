package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;

public class LittleMountainTerrainGenerator extends PathedPlateauTerrainGenerator {
    public LittleMountainTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                                                   .landDensity(.5f, 1f)
                                                   .mountainDensity(.25f, 1)
                                                   .plateauDensity(0, .5f)
                                                   .build();
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        mountainBrushSize = 24;
        mountainBrushDensity = .35f;
        mountainBrushIntensity = 8;
    }

    @Override
    protected void mountainSetup() {
        int mapSize = map.getSize();
        float normalizedMountainDensity = parameterConstraints.getMountainDensityRange()
                                                              .normalize(generatorParameters.mountainDensity());
        mountains.setSize(mapSize / 4);

        mountains.randomWalk(
                (int) (normalizedMountainDensity * 250 / symmetrySettings.getTerrainSymmetry().getNumSymPoints() + 100),
                mapSize / 128);

        mountains.setSize(mapSize + 1);
    }
}
