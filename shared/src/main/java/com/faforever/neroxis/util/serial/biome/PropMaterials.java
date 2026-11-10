package com.faforever.neroxis.util.serial.biome;

import java.util.List;

public record PropMaterials(
        List<PropMaterial> treeGroups,
        List<PropMaterial> rocks,
        List<PropMaterial> boulders
) {
    // Mass and energy values are retrieved from the blueprint files in env.nx2 and faforever.faf.
    public static final List<PropMaterial> OAK_AND_PINE_TREE_GROUPS = List.of(
            new PropMaterial("/env/evergreen/props/trees/groups/Oak01_Group1_prop.bp", 64, 6.4),
            new PropMaterial("/env/evergreen/props/trees/groups/Oak01_Group2_prop.bp", 80, 8),
            new PropMaterial("/env/evergreen/props/trees/groups/Pine06_GroupA_prop.bp", 68.4, 6),
            new PropMaterial("/env/evergreen/props/trees/groups/Pine06_GroupB_prop.bp", 62.7, 6.5),
            new PropMaterial("/env/evergreen/props/trees/groups/Pine07_GroupA_prop.bp", 40, 8),
            new PropMaterial("/env/evergreen/props/trees/groups/Pine07_GroupB_prop.bp", 25, 5)
    );

    public PropMaterials {
        treeGroups = List.copyOf(treeGroups);
        rocks = List.copyOf(rocks);
        boulders = List.copyOf(boulders);
    }

    public record PropMaterial(String path, double energy, double mass) {}
}
