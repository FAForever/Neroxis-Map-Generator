package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ForrestSomethingPropGenerator extends BasicPropGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
    }

    @Override
    public void placePropsWithExclusion() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            Biome biome = map.getBiome();
            // Ensure that we always have the Pine and Oak tree groups in the list for any biome
            List<String> biomeTreeList = biome.propMaterials().treeGroups();
            List<String> oakAndPrineTreeList = Arrays.asList(
                    "/env/evergreen/props/trees/groups/Oak01_Group1_prop.bp",
                    "/env/evergreen/props/trees/groups/Oak01_Group2_prop.bp",
                    "/env/evergreen/props/trees/groups/Pine06_GroupA_prop.bp",
                    "/env/evergreen/props/trees/groups/Pine06_GroupB_prop.bp",
                    "/env/evergreen/props/trees/groups/Pine07_GroupA_prop.bp",
                    "/env/evergreen/props/trees/groups/Pine07_GroupB_prop.bp"
            );
            List<String> treePropsList = Stream.concat(biomeTreeList.stream(), oakAndPrineTreeList.stream())
                    .distinct()
                    .toList();


            propPlacer.placeProps(passableLand.getFinalMask(), treePropsList,2f, 5f, false);
            propPlacer.placeProps(passableLand.getFinalMask(), biome.propMaterials().rocks(), 2f, 4f, false);
            propPlacer.placeProps(cliffRockMask.getFinalMask(), biome.propMaterials().rocks(), .5f, 2.5f, false);
        });
    }

    @Override
    public void setupPipeline() {
        setupCliffAndFieldPipeline();
    }

    protected void setupCliffAndFieldPipeline() {
        int mapSize = map.getSize();
        cliffRockMask.setSize(mapSize / 16);

        cliffRockMask.randomize((reclaimDensity * .75f + random.nextFloat() * .25f) * .5f).setSize(mapSize + 1);
        cliffRockMask.multiply(impassable).dilute(.5f, 6).subtract(impassable).multiply(passableLand);
    }
}
