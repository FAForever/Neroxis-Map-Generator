package com.faforever.neroxis.util.serial.biome;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;

import java.util.List;


@CompiledJson
public record DecalMaterials(
        @JsonAttribute(nullable = false) List<String> fieldNormals,
        @JsonAttribute(nullable = false) List<String> fieldAlbedos,
        @JsonAttribute(nullable = false) List<String> mountainNormals,
        @JsonAttribute(nullable = false) List<String> mountainAlbedos,
        @JsonAttribute(nullable = false) List<String> slopeNormals,
        @JsonAttribute(nullable = false) List<String> slopeAlbedos
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
