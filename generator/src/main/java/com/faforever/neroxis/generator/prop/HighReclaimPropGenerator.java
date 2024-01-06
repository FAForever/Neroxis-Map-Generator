package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public class HighReclaimPropGenerator extends BasicPropGenerator {
    protected BooleanMask fieldBoulderMask;

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mountainDensity(.5f, 1f)
                                   .plateauDensity(.5f, 1f)
                                   .rampDensity(0f, .5f)
                                   .reclaimDensity(.4f, 1f)
                                   .build();
    }
    
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        fieldBoulderMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "fieldBoulderMask", true);
    }

    @Override
    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, cliffRockMask, fieldStoneMask, fieldBoulderMask);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            Biome biome = generatorParameters.biome();
            propPlacer.placeProps(treeMask.getFinalMask().subtract(noProps), biome.propMaterials().getTreeGroups(),
                                  3f, 7f);
            propPlacer.placeProps(cliffRockMask.getFinalMask(), biome.propMaterials().getRocks(), .6f, 2.5f);
            propPlacer.placeProps(fieldStoneMask.getFinalMask().subtract(noProps),
                                  biome.propMaterials().getRocks(), .6f, 2.5f);
            propPlacer.placeProps(fieldBoulderMask.getFinalMask().subtract(noProps),
                                  biome.propMaterials().getBoulders(), 5f, 10f);
        });
    }

    @Override
    protected void setupPropPipeline() {
        int mapSize = map.getSize();
        float reclaimDensity = generatorParameters.reclaimDensity();
        int spawnCount = generatorParameters.spawnCount();
        treeMask.setSize(mapSize / 16);
        cliffRockMask.setSize(mapSize / 32);
        fieldStoneMask.setSize(mapSize / 4);
        fieldBoulderMask.setSize(mapSize / 4);

        cliffRockMask.randomize(reclaimDensity * .5f).setSize(mapSize + 1);
        cliffRockMask.multiply(impassable).dilute(.5f, 10).subtract(impassable).multiply(passableLand);
        fieldBoulderMask.randomize(reclaimDensity * spawnCount * .0025f).setSize(mapSize + 1);
        fieldBoulderMask.multiply(passableLand).fillEdge(10, false);
        fieldStoneMask.init(fieldBoulderMask).dilute(.5f, 6).subtract(fieldBoulderMask).erode(.3f);
        treeMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .15f).setSize(mapSize / 4);
        treeMask.inflate(2).erode(.5f);
        treeMask.setSize(mapSize + 1);
        treeMask.multiply(passableLand.copy().deflate(8)).fillEdge(8, false);
    }
}
