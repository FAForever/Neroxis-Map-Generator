package com.faforever.neroxis.util.serial.biome;

import com.dslplatform.json.CompiledJson;
import com.faforever.neroxis.map.CubeMap;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@CompiledJson
public class TerrainMaterials {
    /* texture slots:
    beach and seafloor
    cliff
    cliffAccent
    ground
    groundAccent
    slopes
    debris
    plateau
    macroTexture
    map-wide utility textures (generated)
    */

    public static final int USED_LAYERS_COUNT = 8;
    // engine limitations - must stay 9 and 10 always
    public static final int TERRAIN_TEXTURE_COUNT = 10;
    public static final int TERRAIN_NORMAL_COUNT = 9;
    private String name;
    private List<CubeMap> cubeMaps = new ArrayList<>();
    private String[] texturePaths = new String[TERRAIN_TEXTURE_COUNT];
    private float[] textureScales = new float[TERRAIN_TEXTURE_COUNT];
    private String[] normalPaths = new String[TERRAIN_NORMAL_COUNT];
    private float[] normalScales = new float[TERRAIN_NORMAL_COUNT];
    private String[] previewColors = new String[USED_LAYERS_COUNT];
    private Integer[] terrainTypes = {
            40,  // beach and seafloor
            150,  // cliff
            150,  // cliffAccent
            1,  // ground
            1,  // groundAccent
            1,  // slopes
            1,  // debris
            1,  // plateau
            221,  // shallowWater
            223   // deepWater
    };
}
