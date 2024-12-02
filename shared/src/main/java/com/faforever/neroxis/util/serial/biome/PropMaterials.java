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
        treeGroups = treeGroups == null ? List.of() : List.copyOf(treeGroups);
        rocks = rocks == null ? List.of() : List.copyOf(rocks);
        boulders = boulders == null ? List.of() : List.copyOf(boulders);
    }
}
