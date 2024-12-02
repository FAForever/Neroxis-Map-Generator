package com.faforever.neroxis.util.serial.biome;

import io.avaje.jsonb.Json;

import java.util.List;


@Json
public record DecalMaterials(
        List<String> fieldNormals,
        List<String> fieldAlbedos,
        List<String> mountainNormals,
        List<String> mountainAlbedos,
        List<String> slopeNormals,
        List<String> slopeAlbedos
) {
    public DecalMaterials {
        fieldNormals = fieldNormals == null ? List.of() : List.copyOf(fieldNormals);
        fieldAlbedos = fieldAlbedos == null ? List.of() : List.copyOf(fieldAlbedos);
        mountainNormals = mountainNormals == null ? List.of() : List.copyOf(mountainNormals);
        mountainAlbedos = mountainAlbedos == null ? List.of() : List.copyOf(mountainAlbedos);
        slopeNormals = slopeNormals == null ? List.of() : List.copyOf(slopeNormals);
        slopeAlbedos = slopeAlbedos == null ? List.of() : List.copyOf(slopeAlbedos);
    }
}
