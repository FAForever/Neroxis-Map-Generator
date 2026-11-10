package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.serial.biome.PropMaterials;
import com.faforever.neroxis.util.serial.biome.PropMaterials.PropMaterial;

import java.util.List;
import java.util.stream.Stream;

public class ForrestSomethingPropGenerator extends BasicPropGenerator {

    @Override
    public void placePropsWithExclusion(BooleanMask noProps) {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            Biome biome = map.getBiome();
            // Ensure that we always have the Pine and Oak tree groups in the list for any biome
            List<PropMaterial> biomeTreeList = biome.propMaterials().treeGroups();
            List<PropMaterial> treePropsList = Stream.concat(biomeTreeList.stream(),
                                                             PropMaterials.OAK_AND_PINE_TREE_GROUPS.stream())
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
