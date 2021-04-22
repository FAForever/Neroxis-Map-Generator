package neroxis.generator.prop;

import neroxis.biomes.Biome;
import neroxis.map.SymmetryType;
import neroxis.util.Pipeline;
import neroxis.util.Util;

public abstract strictfp class ReducedNaturalPropGenerator extends BasicPropGenerator {

    protected void setupPropPipeline() {
        int mapSize = map.getSize();
        float reclaimDensity = mapParameters.getReclaimDensity();
        treeMask.setSize(mapSize / 16);
        cliffRockMask.setSize(mapSize / 16);

        cliffRockMask.randomize((reclaimDensity * .75f + random.nextFloat() * .25f) * .25f).setSize(mapSize + 1);
        cliffRockMask.intersect(impassable).dilute(.5f, SymmetryType.SPAWN, 6).minus(impassable).intersect(passableLand);
        treeMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .1f).setSize(mapSize / 4);
        treeMask.inflate(2).erode(.5f, SymmetryType.SPAWN).erode(.5f, SymmetryType.SPAWN);
        treeMask.setSize(mapSize + 1);
        treeMask.intersect(passableLand.copy().deflate(8)).fillEdge(8, false);
    }

    @Override
    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, cliffRockMask, fieldStoneMask);
        Util.timedRun("neroxis.generator", "placeProps", () -> {
            Biome biome = mapParameters.getBiome();
            propPlacer.placeProps(treeMask.getFinalMask().minus(noProps), biome.getPropMaterials().getTreeGroups(), 3f, 7f);
            propPlacer.placeProps(cliffRockMask.getFinalMask(), biome.getPropMaterials().getRocks(), .5f, 3.5f);
        });
    }
}
