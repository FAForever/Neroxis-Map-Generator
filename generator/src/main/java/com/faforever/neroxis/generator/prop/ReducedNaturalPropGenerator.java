package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public abstract class ReducedNaturalPropGenerator extends BasicPropGenerator {
    @Override
    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, cliffRockMask, fieldStoneMask);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            Biome biome = generatorParameters.biome();
            propPlacer.placeProps(treeMask.getFinalMask().subtract(noProps), biome.propMaterials().treeGroups(),
                                  3f, 7f);
            propPlacer.placeProps(cliffRockMask.getFinalMask(), biome.propMaterials().rocks(), .5f, 3.5f);
        });
    }

    @Override
    protected void setupPropPipeline() {
        int mapSize = map.getSize();
        float reclaimDensity = random.nextFloat();
        treeMask.setSize(mapSize / 16);
        cliffRockMask.setSize(mapSize / 16);

        cliffRockMask.randomize((reclaimDensity * .75f + random.nextFloat() * .25f) * .25f).setSize(mapSize + 1);
        cliffRockMask.multiply(impassable).dilute(.5f, 6).subtract(impassable).multiply(passableLand);
        treeMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .1f).setSize(mapSize / 4);
        treeMask.inflate(2).erode(.5f);
        treeMask.setSize(mapSize + 1);
        treeMask.multiply(passableLand.copy().deflate(8)).fillEdge(8, false);
    }
}
