package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public class BoulderFieldPropGenerator extends BasicPropGenerator {
    protected BooleanMask fieldBoulderMask;
    protected BooleanMask boulderReclaimAreaMask;
    protected BooleanMask stoneReclaimAreaMask;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        fieldBoulderMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "fieldBoulderMask", true);
        boulderReclaimAreaMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "boulderReclaimAreaMask",
                                                 true);
        stoneReclaimAreaMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "stoneReclaimAreaMask", true);
    }

    @Override
    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, fieldStoneMask, fieldBoulderMask, stoneReclaimAreaMask,
                       boulderReclaimAreaMask);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            Biome biome = map.getBiome();
            propPlacer.placeProps(treeMask.getFinalMask().subtract(noProps), biome.propMaterials().treeGroups(),
                                  3f, 7f);
            propPlacer.placeProps(fieldStoneMask.getFinalMask().subtract(noProps),
                                  biome.propMaterials().rocks(), .5f, 2.5f);
            propPlacer.placeProps(fieldBoulderMask.getFinalMask().subtract(noProps),
                                  biome.propMaterials().boulders(), 5f, 10f);
            propPlacer.placeProps(stoneReclaimAreaMask.getFinalMask().subtract(noProps),
                                  biome.propMaterials().rocks(), 1f, 3f);
            propPlacer.placeProps(boulderReclaimAreaMask.getFinalMask().subtract(noProps),
                                  biome.propMaterials().boulders(), 4f, 7f);
        });
    }

    @Override
    protected void setupPropPipeline() {
        int mapSize = map.getSize();
        float naturalReclaimDensity = reclaimDensity * 0.7f + 0.3f;
        int spawnCount = generatorParameters.spawnCount();
        treeMask.setSize(mapSize / 16);
        fieldBoulderMask.setSize(mapSize / 4);
        boulderReclaimAreaMask.setSize(mapSize / 4);

        BooleanMask reclaimArea = new BooleanMask(1, random.nextLong(), symmetrySettings, "reclaimArea", true);
        reclaimArea.setSize(mapSize / 4);
        reclaimArea.randomize(naturalReclaimDensity * spawnCount * .0003f).dilute(.8f, 4).setSize(mapSize + 1);
        boulderReclaimAreaMask.randomize(0.5f).setSize(mapSize + 1).multiply(reclaimArea);
        stoneReclaimAreaMask.init(boulderReclaimAreaMask).dilute(.5f, 2).subtract(boulderReclaimAreaMask).dilute(.5f);
        boulderReclaimAreaMask.multiply(passableLand).fillEdge(10, false);
        stoneReclaimAreaMask.multiply(passableLand).fillEdge(9, false);

        fieldBoulderMask.randomize(naturalReclaimDensity * spawnCount * .00025f).setSize(mapSize + 1);
        fieldBoulderMask.multiply(passableLand).fillEdge(10, false);
        fieldStoneMask.init(fieldBoulderMask).dilute(.5f, 6).subtract(fieldBoulderMask).erode(.3f);
        treeMask.randomize((naturalReclaimDensity + random.nextFloat()) / 2f * .15f).setSize(mapSize / 4);
        treeMask.inflate(2).erode(.5f);
        treeMask.setSize(mapSize + 1);
        treeMask.subtract(boulderReclaimAreaMask)
                .subtract(stoneReclaimAreaMask.copy().dilute(.8f, 2))
                .subtract(fieldBoulderMask);
        treeMask.multiply(passableLand.copy().deflate(8)).fillEdge(8, false);
    }
}