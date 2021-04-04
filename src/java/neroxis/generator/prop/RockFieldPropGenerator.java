package neroxis.generator.prop;

import neroxis.biomes.Biome;
import neroxis.generator.ParameterConstraints;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

public class RockFieldPropGenerator extends DefaultPropGenerator {

    protected ConcurrentBinaryMask largeRockFieldMask;

    public RockFieldPropGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .reclaimDensity(.25f, 1f)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        largeRockFieldMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "largeRockFieldMask");
    }

    @Override
    public void setupPipeline() {
        super.setupPipeline();
        setupRockFieldPipeline();
    }

    protected void setupRockFieldPipeline() {
        int mapSize = map.getSize();
        float reclaimDensity = mapParameters.getReclaimDensity();
        largeRockFieldMask.setSize(mapSize / 4);

        largeRockFieldMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .00075f).fillEdge(32, false).grow(.5f, SymmetryType.SPAWN, 8).setSize(mapSize + 1);
        largeRockFieldMask.intersect(passableLand);
    }

    @Override
    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, cliffRockMask, largeRockFieldMask, fieldStoneMask);
        long sTime = System.currentTimeMillis();
        Biome biome = mapParameters.getBiome();
        propPlacer.placeProps(treeMask.getFinalMask().minus(noProps), biome.getPropMaterials().getTreeGroups(), 3f, 7f);
        propPlacer.placeProps(cliffRockMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), .5f, 3f);
        propPlacer.placeProps(largeRockFieldMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), .5f, 3.5f);
        propPlacer.placeProps(fieldStoneMask.getFinalMask().minus(noProps), biome.getPropMaterials().getBoulders(), 20f);
        if (DEBUG) {
            System.out.printf("Done: %4d ms, %s, placeProps\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInClass(neroxis.generator.style.DefaultStyleGenerator.class));
        }
    }
}
