package com.faforever.neroxis.map;

import com.dslplatform.json.CompiledJson;
import lombok.Data;

@Data
@CompiledJson
public strictfp class TerrainMaterials {

    // engine limitations - must stay 9 and 10 always
    public static final int TERRAIN_TEXTURE_COUNT = 10;
    public static final int TERRAIN_NORMAL_COUNT = 9;

    private String name;
    private String[] texturePaths = new String[TERRAIN_TEXTURE_COUNT];
    private float[] textureScales = new float[TERRAIN_TEXTURE_COUNT];
    private String[] normalPaths = new String[TERRAIN_NORMAL_COUNT];
    private float[] normalScales = new float[TERRAIN_NORMAL_COUNT];
    private Integer[] previewColors = new Integer[TERRAIN_NORMAL_COUNT];

}
