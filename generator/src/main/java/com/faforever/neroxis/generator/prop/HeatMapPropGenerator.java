package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public class HeatMapPropGenerator extends BasicPropGenerator {

    protected FloatMask resourceDensityMap;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator, pipeline);
        pipeline.setDebug(true);
        resourceDensityMap = terrainGenerator.getResourceDensityMap();
    }

    @Override
    public void setupPipeline() {
    }

    @Override
    public void placeProps() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            generatePropExclusionMasks();

            if (resourceDensityMap == null) {
                // Create a heatmap there isn't one supplied by the Terrain Generator
                BooleanMask heatExlcusion = noProps.copy().inflate(8);
                resourceDensityMap = passableLand.copyAsFloatMask(0, 1);
                resourceDensityMap.setVisualName("Fallback Heatmap");
                resourceDensityMap.startVisualDebugger();
                resourceDensityMap.setValue(heatExlcusion, 0)
                                  .blur(20)
                                  .setValue(heatExlcusion, 0)
                                  .blur(25)
                                  .setValue(heatExlcusion, 0)
                                  .blur(30);
                resourceDensityMap.scaleToNewMinAndMaxHeight(0, 1);
            }

            resourceDensityMap.setValue(noProps.copy().inflate(4), 0);
            Biome biome = map.getBiome();
            propPlacer.placeProps(resourceDensityMap, biome.propMaterials(), reclaimDensity);
        });
    }

    @Override
    public void placeUnits() {
        if (resourceDensityMap == null) {
            super.placeUnits();
        }
    }
}
