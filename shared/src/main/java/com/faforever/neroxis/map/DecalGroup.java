package com.faforever.neroxis.map;

import java.util.List;


public record DecalGroup(
        String name,
        List<Integer> data
) {
    public DecalGroup {
        data = List.copyOf(data);
    }
}

