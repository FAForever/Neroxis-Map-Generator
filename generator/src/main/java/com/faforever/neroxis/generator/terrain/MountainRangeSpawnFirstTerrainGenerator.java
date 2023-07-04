package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;

public class MountainRangeSpawnFirstTerrainGenerator extends PathedPlateauSpawnFirstTerrainGenerator {
    public MountainRangeSpawnFirstTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                                                   .landDensity(.75f, 1f)
                                                   .mountainDensity(.5f, 1)
                                                   .mexDensity(.375f, 1)
                                                   .mapSizes(256, 768)
                                                   .build();
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        mountainBrushSize = map.getSize() / 16;
        mountainBrushDensity = 1.25f;
        mountainBrushIntensity = 3f;
    }

    @Override
    protected void mountainSetup() {
        int mapSize = map.getSize();
        float normalizedMountainDensity = parameterConstraints.getMountainDensityRange()
                                                              .normalize(generatorParameters.mountainDensity());
        mountains.setSize(mapSize / 2);

        mountains.progressiveWalk(
                (int) (normalizedMountainDensity * 16 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()) + 8,
                mapSize / 4);
        mountains.inflate(2);

        mountains.setSize(mapSize + 1);
    }
}
