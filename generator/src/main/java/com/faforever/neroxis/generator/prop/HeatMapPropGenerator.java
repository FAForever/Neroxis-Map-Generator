package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.HeatMapPropPlacer;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.DebugUtil;

import java.util.random.RandomGenerator;

public class HeatMapPropGenerator extends BasicPropGenerator {

    private FloatMask reclaimHeatMap;
    private FloatMask reclaimExclusion;

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, random, generatorParameters, symmetrySettings, terrainGenerator);
        reclaimHeatMap = new FloatMask(1, random.split(), symmetrySettings, "resourceHeatMap");
        reclaimExclusion = new FloatMask(1, random.split(), symmetrySettings, "resourceExclusion");
        propPlacer = new HeatMapPropPlacer(map, random.split());
    }

    @Override
    public void setupPipeline() {
        setupHeatmapPipeline();
    }

    public void setupHeatmapPipeline() {
        int mapSize = map.getSize();

        // Create a Heatmap of resources (trees, rock and boulders)
        reclaimHeatMap.setSize(mapSize + 1);
        float heatmapOctaveMultiplier = 1.0f;
        float amplitude = 1f;
        int numOctaves = 7;
        for (int octave = 0; octave < numOctaves; octave++) {
            FloatMask octaveNoise = new FloatMask(mapSize + 1, random.split(),
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
        FloatMask midReducer = new FloatMask(mapSize + 1, random.split(), symmetrySettings, "midReducer");
        midReducer.fillCircle((float) mapSize / 2, (float) mapSize / 2, (float) mapSize / 6, 1f);
        midReducer.blur(mapSize / 6);
        reclaimHeatMap.subtract(midReducer)
                      .clampMin(0f);

        // Create some random paths where resources should not generate
        reclaimExclusion.setSize(mapSize + 1);
        reclaimExclusion.addPerlinNoise(128, 1);
        reclaimHeatMap.subtract(reclaimExclusion
                                        .copyAsBooleanMask(0.4f, 0.5f)
                                        .copyAsFloatMask(0f, 1f)
                                        .blur(10))
                      .clampMin(0f);
    }

    @Override
    protected void generatePropExclusionMasks() {
        noProps.init(passableLand
                             .getFinalMask()
                             .invert()
                             .inflate(5));

        generateExclusionZones(noProps, 30, 2, 8);
    }

    @Override
    public void placePropsWithExclusion() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            // Subtract the area where props aren't allowed (Mountains, spawns, mexes, hydros)
            reclaimHeatMap.setToValue(noProps, 0f);

            // Place the props
            Biome biome = map.getBiome();
            ((HeatMapPropPlacer) propPlacer).placeProps(reclaimHeatMap, biome.propMaterials(), reclaimDensity);
        });
    }

}
