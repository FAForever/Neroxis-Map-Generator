package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public class RockFieldPropGenerator extends BasicPropGenerator {
    protected BooleanMask largeRockFieldMask;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        largeRockFieldMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "largeRockFieldMask", true);
    }

    @Override
    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, cliffRockMask, largeRockFieldMask, fieldStoneMask);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            Biome biome = map.getBiome();
            propPlacer.placeProps(treeMask.getFinalMask().subtract(noProps), biome.propMaterials().treeGroups(),
                                  3f, 7f, false);
            propPlacer.placeProps(cliffRockMask.getFinalMask().subtract(noProps), biome.propMaterials().rocks(),
                                  .5f, 3f, false);
            propPlacer.placeProps(largeRockFieldMask.getFinalMask().subtract(noProps),
                                  biome.propMaterials().rocks(), .5f, 3.5f, false);
            propPlacer.placeProps(fieldStoneMask.getFinalMask().subtract(noProps),
                                  biome.propMaterials().boulders(), 20f, true);
        });
    }

    @Override
    public void setupPipeline() {
        super.setupPipeline();
        setupRockFieldPipeline();
    }

    protected void setupRockFieldPipeline() {
        int mapSize = map.getSize();
        float fieldDensity = reclaimDensity * 0.8f + 0.2f;
        largeRockFieldMask.setSize(mapSize / 4);

        largeRockFieldMask.randomize((fieldDensity) * .00075f)
                          .fillEdge(32, false)
                          .dilute(.5f, 8)
                          .setSize(mapSize + 1);
        largeRockFieldMask.multiply(passableLand);
    }
}
