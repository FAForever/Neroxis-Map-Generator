package com.faforever.neroxis.util.serial.biome;

import com.faforever.neroxis.map.CubeMap;
import io.avaje.jsonb.Json;

import java.util.List;
import java.util.Objects;

@Json
public record TerrainMaterials(
        List<CubeMap> cubeMaps,
        List<TextureScale> textures,
        List<TextureScale> normals,
        List<String> previewColors,
        List<Integer> terrainTypes
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

    @Json
    public record TextureScale(
            String path,
            float scale
    ) {
        public TextureScale {
            Objects.requireNonNull(path);
        }
    }
}