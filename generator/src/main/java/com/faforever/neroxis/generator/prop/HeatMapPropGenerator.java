package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.DebugUtil;

public class HeatMapPropGenerator extends BasicPropGenerator {

    private FloatMask reclaimHeatMap;
    private FloatMask reclaimExclusion;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        reclaimHeatMap = new FloatMask(1, random.nextLong(), symmetrySettings, "resourceHeatMap");
        reclaimExclusion = new FloatMask(1, random.nextLong(), symmetrySettings, "resourceExclusion");
    }

    @Override
    public void setupPipeline() {
    }

    @Override
    public void placeProps() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            int mapSize = map.getSize();

            // Create a Heatmap of resources (trees, rock and boulders)
            reclaimHeatMap.setSize(mapSize + 1).startVisualDebugger();
            float heatmapOctaveMultiplier = 1.0f;
            float amplitude = 1f;
            int numOctaves = 7;
            for (int octave = 0; octave < numOctaves; octave++) {
                FloatMask octaveNoise = new FloatMask(mapSize + 1, getRandom().nextLong(),
                                                      reclaimHeatMap.getSymmetrySettings(),
                                                      "resourceHeatMapOctave" + octave);
                octaveNoise.addPerlinNoise(2 << octave, 1f / numOctaves);
                octaveNoise.multiply(amplitude);
                reclaimHeatMap.add(octaveNoise);
                amplitude *= heatmapOctaveMultiplier;
            }
            reclaimHeatMap.blur(8);
            reclaimHeatMap.scaleToNewMinAndMaxHeight(0, 1);

            // Reduce the probability of reclaim in the middle of the map
            FloatMask midReducer = new FloatMask(mapSize + 1, random.nextLong(), symmetrySettings,
                                                 "midReducer").startVisualDebugger();
            midReducer.fillCircle((float) mapSize / 2, (float) mapSize / 2, (float) mapSize / 6, 1f);
            midReducer.blur(mapSize / 6);
            reclaimHeatMap.subtractWithMin(midReducer, 0f);

            // Create some random paths where resources should not generate
            reclaimExclusion.setSize(mapSize + 1).startVisualDebugger();
            reclaimExclusion.addPerlinNoise(128, 1);
            reclaimHeatMap.subtractWithMin(reclaimExclusion
                                                    .copyAsBooleanMask(0.4f, 0.5f)
                                                    .copyAsFloatMask(0f, 1f).startVisualDebugger("exclusion:")
                                                    .blur(10),
                                           0f
            );

            // Subtract the area where props aren't allowed (Mountains and spawns)
            reclaimHeatMap.subtractWithMin(passableLand
                                                    .getFinalMask()
                                                    .invert()
                                                    .inflate(5)
                                                    .copyAsFloatMask(0f, 1f)
                                                    .blur(3),
                                           0f
            );

            // Place the props
            Biome biome = map.getBiome();
            propPlacer.placeProps(reclaimHeatMap, biome.propMaterials(), reclaimDensity);
        });
    }

    @Override
    public void placeUnits() {
        if (reclaimHeatMap == null) {
            super.placeUnits();
        }
    }
}
