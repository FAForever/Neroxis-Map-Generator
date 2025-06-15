package com.faforever.neroxis.util.serial.biome;

import io.avaje.jsonb.Json;

import java.util.List;

@Json
public record PropMaterials(
        List<String> treeGroups,
        List<String> rocks,
        List<String> boulders
) {
    public PropMaterials {
        treeGroups = List.copyOf(treeGroups);
        rocks = List.copyOf(rocks);
        boulders = List.copyOf(boulders);
    }
}
