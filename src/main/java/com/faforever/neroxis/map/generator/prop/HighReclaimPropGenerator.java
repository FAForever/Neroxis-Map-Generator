package com.faforever.neroxis.map.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.Util;

public class HighReclaimPropGenerator extends BasicPropGenerator {

    public HighReclaimPropGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .mountainDensity(.5f, 1f)
                .plateauDensity(.5f, 1f)
                .rampDensity(0f, .5f)
                .reclaimDensity(.8f, 1f)
                .biomes("Desert", "Frithen", "Loki", "Moonlight", "Wonder")
                .build();
        weight = 2;
    }

    protected void setupPropPipeline() {
        int mapSize = map.getSize();
        float reclaimDensity = mapParameters.getReclaimDensity();
        treeMask.setSize(mapSize / 16);
        cliffRockMask.setSize(mapSize / 16);
        fieldStoneMask.setSize(mapSize / 4);

        cliffRockMask.randomize((reclaimDensity * .75f + random.nextFloat() * .25f) * .5f).setSize(mapSize + 1);
        cliffRockMask.intersect(impassable).dilute(.5f, SymmetryType.SPAWN, 12).minus(impassable).intersect(passableLand);
        fieldStoneMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .0025f).setSize(mapSize + 1);
        fieldStoneMask.intersect(passableLand).fillEdge(10, false);
        treeMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .15f).setSize(mapSize / 4);
        treeMask.inflate(2).erode(.5f, SymmetryType.SPAWN).erode(.5f, SymmetryType.SPAWN);
        treeMask.setSize(mapSize + 1);
        treeMask.intersect(passableLand.copy().deflate(8)).fillEdge(8, false);
    }

    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, cliffRockMask, fieldStoneMask);
        Util.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            Biome biome = mapParameters.getBiome();
            propPlacer.placeProps(treeMask.getFinalMask().minus(noProps), biome.getPropMaterials().getTreeGroups(), 3f, 7f);
            propPlacer.placeProps(cliffRockMask.getFinalMask(), biome.getPropMaterials().getBoulders(), 3f, 8f);
            propPlacer.placeProps(fieldStoneMask.getFinalMask().minus(noProps), biome.getPropMaterials().getBoulders(), 30f);
        });
    }

    @Override
    public void placeUnits() {
    }
}
