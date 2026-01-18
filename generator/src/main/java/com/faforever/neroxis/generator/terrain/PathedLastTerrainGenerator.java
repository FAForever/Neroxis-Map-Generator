package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.util.Pipeline;

public abstract class PathedLastTerrainGenerator extends BasicLastTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
    }

    @Override
    protected void initRamps() {
        int mapSize = map.getSize();
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 2;
        int numPaths = (int) (rampDensity * 20) / symmetrySettings.terrainSymmetry().getNumSymPoints();
        int bound = mapSize / 4;
        ramps.setSize(mapSize + 1);

        MapMaskMethods.pathInEdgeBounds(random.nextLong(), ramps, maxStepSize, numPaths, maxMiddlePoints, bound,
                                        (float) (StrictMath.PI / 2));
        MapMaskMethods.pathInCenterBounds(random.nextLong(), ramps, maxStepSize, numPaths / 2, maxMiddlePoints, bound,
                                          (float) (StrictMath.PI / 2));

        ramps.subtract(connections.copy().inflate(32))
             .inflate(maxStepSize / 2f)
             .multiply(plateaus.copy().outline())
             .add(connections.copy().inflate(maxStepSize / 2f).multiply(plateaus.copy().outline()))
             .subtract(mountains)
             .inflate(8);
    }
}


