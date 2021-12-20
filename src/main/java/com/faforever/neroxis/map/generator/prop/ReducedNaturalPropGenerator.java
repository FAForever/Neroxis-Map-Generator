package com.faforever.neroxis.map.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.util.DebugUtils;
import com.faforever.neroxis.util.Pipeline;

public abstract strictfp class ReducedNaturalPropGenerator extends BasicPropGenerator {

    protected void setupPropPipeline() {
        int mapSize = map.getSize();
        float reclaimDensity = mapParameters.getReclaimDensity();
        treeMask.setSize(mapSize / 16);
        cliffRockMask.setSize(mapSize / 16);

        cliffRockMask.randomize((reclaimDensity * .75f + random.nextFloat() * .25f) * .25f).setSize(mapSize + 1);
        cliffRockMask.multiply(impassable).dilute(.5f, 6).subtract(impassable).multiply(passableLand);
        treeMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .1f).setSize(mapSize / 4);
        treeMask.inflate(2).erode(.5f);
        treeMask.setSize(mapSize + 1);
        treeMask.multiply(passableLand.copy().deflate(8)).fillEdge(8, false);
    }

    @Override
    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, cliffRockMask, fieldStoneMask);
        DebugUtils.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            Biome biome = mapParameters.getBiome();
            propPlacer.placeProps(treeMask.getFinalMask().subtract(noProps), biome.getPropMaterials().getTreeGroups(), 3f, 7f);
            propPlacer.placeProps(cliffRockMask.getFinalMask(), biome.getPropMaterials().getRocks(), .5f, 3.5f);
        });
    }
}
