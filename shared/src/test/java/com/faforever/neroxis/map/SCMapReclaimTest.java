package com.faforever.neroxis.map;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.util.serial.biome.PropMaterials;
import com.faforever.neroxis.util.vector.Vector2;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SCMapReclaimTest {
    @Test
    public void emptyMapHasCompleteZeroTotals() {
        SCMap map = new SCMap(16, Biomes.loadBiome(BiomeName.BRIMSTONE));

        assertEquals(Optional.of(new SCMap.Reclaim(0, 0, 0, 0)), map.getReclaim());
    }

    @Test
    public void knownPropsAndWreckageHaveCompleteTotals() {
        Biome biome = Biomes.loadBiome(BiomeName.BRIMSTONE);
        SCMap map = new SCMap(16, biome);
        PropMaterials.PropMaterial material = biome.propMaterials().rocks().getFirst();
        map.addProp(new Prop(material.path(), new Vector2(0, 0), 0, false));
        map.addProp(new Prop(PropMaterials.OAK_AND_PINE_TREE_GROUPS.get(1).path(), new Vector2(0, 0), 0, false));
        addWreckage(map, "XEL0305");

        SCMap.Reclaim reclaim = map.getReclaim().orElseThrow();
        assertEquals(material.mass() + 8, reclaim.propMass());
        assertEquals(material.energy() + 80, reclaim.propEnergy());
        assertEquals(1152, reclaim.wreckageMass());
        assertEquals(0, reclaim.wreckageEnergy());
        assertEquals(material.mass() + 8 + 1152, reclaim.mass());
        assertEquals(material.energy() + 80, reclaim.energy());
    }

    @Test
    public void missingImportedPropMetadataMakesAllTotalsUnavailable() {
        Biome biome = Biomes.loadBiome(BiomeName.BRIMSTONE);
        SCMap map = new SCMap(16, new Biome(null, biome.terrainMaterials(),
                                          new PropMaterials(List.of(), List.of(), List.of()), biome.decalMaterials(),
                                          biome.waterSettings(), biome.lightingSettings()));
        map.addProp(new Prop(PropMaterials.OAK_AND_PINE_TREE_GROUPS.get(1).path(), new Vector2(0, 0), 0, false));
        map.addProp(new Prop(biome.propMaterials().rocks().getFirst().path(), new Vector2(0, 0), 0, false));
        addWreckage(map, "XEL0305");

        assertTrue(map.getReclaim().isEmpty());
    }

    @Test
    public void unknownWreckageMakesAllTotalsUnavailable() {
        SCMap map = new SCMap(16, Biomes.loadBiome(BiomeName.BRIMSTONE));
        map.addProp(new Prop(PropMaterials.OAK_AND_PINE_TREE_GROUPS.get(1).path(), new Vector2(0, 0), 0, false));
        addWreckage(map, "XEL0305");
        addWreckage(map, "unknown");

        assertTrue(map.getReclaim().isEmpty());
    }

    private static void addWreckage(SCMap map, String type) {
        Army army = new Army("ARMY_17");
        Group group = new Group("WRECKAGE");
        group.addUnit(new Unit("wreck", type, new Vector2(0, 0), 0));
        army.addGroup(group);
        map.addArmy(army);
    }
}
