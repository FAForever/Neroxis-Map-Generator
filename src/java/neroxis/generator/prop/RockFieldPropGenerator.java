package neroxis.generator.prop;

import neroxis.biomes.Biome;
import neroxis.generator.ParameterConstraints;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

public class RockFieldPropGenerator extends BasicPropGenerator {

    protected BinaryMask largeRockFieldMask;

    public RockFieldPropGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .reclaimDensity(.25f, 1f)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        largeRockFieldMask = new BinaryMask(1, random.nextLong(), symmetrySettings, "largeRockFieldMask", true);
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

        largeRockFieldMask.randomize((reclaimDensity * .75f + random.nextFloat() * .25f) * .00075f).fillEdge(32, false).dilute(.5f, SymmetryType.SPAWN, 8).setSize(mapSize + 1);
        largeRockFieldMask.intersect(passableLand);
    }

    @Override
    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, cliffRockMask, largeRockFieldMask, fieldStoneMask);
        Util.timedRun("neroxis.generator", "placeProps", () -> {
            Biome biome = mapParameters.getBiome();
            propPlacer.placeProps(((BinaryMask) treeMask.getFinalMask()).minus(noProps), biome.getPropMaterials().getTreeGroups(), 3f, 7f);
            propPlacer.placeProps(((BinaryMask) cliffRockMask.getFinalMask()).minus(noProps), biome.getPropMaterials().getRocks(), .5f, 3f);
            propPlacer.placeProps(((BinaryMask) largeRockFieldMask.getFinalMask()).minus(noProps), biome.getPropMaterials().getRocks(), .5f, 3.5f);
            propPlacer.placeProps(((BinaryMask) fieldStoneMask.getFinalMask()).minus(noProps), biome.getPropMaterials().getBoulders(), 20f);
        });
    }
}
