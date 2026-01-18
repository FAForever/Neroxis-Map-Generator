package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.Pipeline;

public class MountainRangeLastTerrainGenerator extends PathedPlateauLastTerrainGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mapSizes(256, 768)
                                   .build();
    }


    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
        mountainBrushSize = map.getSize() / 16;
        mountainBrushDensity = 1.25f;
        mountainBrushIntensity = 3f;
    }

    @Override
    protected void mountainSetup() {
        int mapSize = map.getSize();
        mountains.setSize(mapSize / 2);

        mountains.progressiveWalk(
                (int) (mountainDensity * 8 / symmetrySettings.terrainSymmetry().getNumSymPoints()) + 4,
                mapSize / 4);
        mountains.inflate(2);

        mountains.setSize(mapSize + 1);
    }
}
