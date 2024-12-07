package com.faforever.neroxis.util.serial.biome;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;

import java.util.List;

@CompiledJson
public record PropMaterials(
        @JsonAttribute(nullable = false) List<String> treeGroups,
        @JsonAttribute(nullable = false) List<String> rocks,
        @JsonAttribute(nullable = false) List<String> boulders
) {
    public PropMaterials {
        treeGroups = List.copyOf(treeGroups);
        rocks = List.copyOf(rocks);
        boulders = List.copyOf(boulders);
    }
}
