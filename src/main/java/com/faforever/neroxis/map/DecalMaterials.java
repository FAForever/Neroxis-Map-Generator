package com.faforever.neroxis.map;

import com.dslplatform.json.CompiledJson;
import lombok.Data;

@Data
@CompiledJson
public strictfp class DecalMaterials {
    private String[] fieldNormals;
    private String[] fieldAlbedos;
    private String[] mountainNormals;
    private String[] mountainAlbedos;
    private String[] slopeNormals;
    private String[] slopeAlbedos;
}
