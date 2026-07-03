package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.Visibility;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.HeatMapPropPlacer;
import com.faforever.neroxis.map.placement.PropPlacer;
import com.faforever.neroxis.map.placement.UnitPlacer;
import com.faforever.neroxis.mask.BooleanMask;
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
        propPlacer = new HeatMapPropPlacer(map, random.nextLong());
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
        FloatMask midReducer = new FloatMask(mapSize + 1, random.nextLong(), symmetrySettings,"midReducer");
        midReducer.fillCircle((float) mapSize / 2, (float) mapSize / 2, (float) mapSize / 6, 1f);
        midReducer.blur(mapSize / 6);
        reclaimHeatMap.subtractWithMin(midReducer, 0f);

        // Create some random paths where resources should not generate
        reclaimExclusion.setSize(mapSize + 1);
        reclaimExclusion.addPerlinNoise(128, 1);
        reclaimHeatMap.subtractWithMin(reclaimExclusion
                                               .copyAsBooleanMask(0.4f, 0.5f)
                                               .copyAsFloatMask(0f, 1f)
                                               .blur(10),
                                       0f
        );
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
