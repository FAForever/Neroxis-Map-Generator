package com.faforever.neroxis.util.serial.biome;

import java.util.List;

public record DecalMaterials(
        List<String> fieldNormals,
        List<String> fieldAlbedos,
        List<String> mountainNormals,
        List<String> mountainAlbedos,
        List<String> slopeNormals,
        List<String> slopeAlbedos
) {
    public DecalMaterials {
        fieldNormals = List.copyOf(fieldNormals);
        fieldAlbedos = List.copyOf(fieldAlbedos);
        mountainNormals = List.copyOf(mountainNormals);
        mountainAlbedos = List.copyOf(mountainAlbedos);
        slopeNormals = List.copyOf(slopeNormals);
        slopeAlbedos = List.copyOf(slopeAlbedos);
    }
}
