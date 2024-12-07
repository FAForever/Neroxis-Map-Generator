package com.faforever.neroxis.util.serial.biome;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import com.faforever.neroxis.map.CubeMap;

import java.util.List;
import java.util.Objects;

@CompiledJson
public record TerrainMaterials(
        @JsonAttribute(mandatory = true, nullable = false) List<CubeMap> cubeMaps,
        @JsonAttribute(mandatory = true, nullable = false) List<TextureScale> textures,
        @JsonAttribute(mandatory = true, nullable = false) List<TextureScale> normals,
        @JsonAttribute(mandatory = true, nullable = false) List<String> previewColors,
        @JsonAttribute(mandatory = true, nullable = false) List<Integer> terrainTypes
) {

    // engine limitations - must stay 9 and 10 always
    public static final int TERRAIN_TEXTURE_COUNT = 10;
    public static final int TERRAIN_NORMAL_COUNT = 9;

    public TerrainMaterials {
        cubeMaps = List.copyOf(cubeMaps);
        terrainTypes = List.copyOf(terrainTypes);
        textures = List.copyOf(textures);
        normals = List.copyOf(normals);
        previewColors = List.copyOf(previewColors);

        if (textures.size() != TERRAIN_TEXTURE_COUNT) {
            throw new IllegalArgumentException("Texture paths does not have 10 items");
        }

        if (normals.size() != TERRAIN_NORMAL_COUNT) {
            throw new IllegalArgumentException("Normal paths does not have 9 items");
        }

    }

    public record TextureScale(
            @JsonAttribute(mandatory = true, nullable = false) String path,
            @JsonAttribute(mandatory = true, nullable = false) float scale
    ) {
        public TextureScale {
            Objects.requireNonNull(path);
        }
    }
}