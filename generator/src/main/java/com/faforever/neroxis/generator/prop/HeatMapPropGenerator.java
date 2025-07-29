package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public class HeatMapPropGenerator extends BasicPropGenerator {

    protected FloatMask resourceDensityMap;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator, pipeline);
        resourceDensityMap = terrainGenerator.getResourceDensityMap();
    }

    @Override
    public void setupPipeline() {
        if (resourceDensityMap == null) {
            super.setupPipeline();
        }
    }

    @Override
    public void placeProps() {
        if (resourceDensityMap == null) {
            super.placeProps();
        } else {
            DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
                generatePropExclusionMasks();
                resourceDensityMap.setToValue(noProps.copy().inflate(4), 0f);
                Biome biome = map.getBiome();
                propPlacer.placeProps(resourceDensityMap, biome.propMaterials(), reclaimDensity);
            });
        }
    }

    @Override
    public void placeUnits() {
        if (resourceDensityMap == null) {
            super.placeUnits();
        }
    }
}
