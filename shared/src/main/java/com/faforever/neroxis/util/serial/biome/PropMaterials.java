package com.faforever.neroxis.util.serial.biome;

import java.util.List;

public record PropMaterials(
        List<PropMaterial> treeGroups,
        List<PropMaterial> rocks,
        List<PropMaterial> boulders
) {
    public PropMaterials {
        treeGroups = List.copyOf(treeGroups);
        rocks = List.copyOf(rocks);
        boulders = List.copyOf(boulders);
    }

    public record PropMaterial(String path, double energy, double mass) {}
}
