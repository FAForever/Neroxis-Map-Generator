package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.serial.biome.PropMaterials.PropMaterial;

import java.util.List;
import java.util.stream.Stream;

public class ForrestSomethingPropGenerator extends BasicPropGenerator {

    @Override
    public void placePropsWithExclusion(BooleanMask noProps) {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            Biome biome = map.getBiome();
            // Ensure that we always have the Pine and Oak tree groups in the list for any biome
            // Mass and energy values are retrieved from the blueprint files in env.nx2 and faforever.faf
            List<PropMaterial> biomeTreeList = biome.propMaterials().treeGroups();
            List<PropMaterial> oakAndPineTreeList = List.of(
                    new PropMaterial("/env/evergreen/props/trees/groups/Oak01_Group1_prop.bp", 64, 6.4),
                    new PropMaterial("/env/evergreen/props/trees/groups/Oak01_Group2_prop.bp", 80, 8),
                    new PropMaterial("/env/evergreen/props/trees/groups/Pine06_GroupA_prop.bp", 68.4, 6),
                    new PropMaterial("/env/evergreen/props/trees/groups/Pine06_GroupB_prop.bp", 62.7, 6.5),
                    new PropMaterial("/env/evergreen/props/trees/groups/Pine07_GroupA_prop.bp", 40, 8),
                    new PropMaterial("/env/evergreen/props/trees/groups/Pine07_GroupB_prop.bp", 25, 5)
            );
            List<PropMaterial> treePropsList = Stream.concat(biomeTreeList.stream(), oakAndPineTreeList.stream())
                                                     .distinct()
                                                     .toList();


            propPlacer.placeProps(passableLand.getFinalMask(), treePropsList, 2f, 5f, false);
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
