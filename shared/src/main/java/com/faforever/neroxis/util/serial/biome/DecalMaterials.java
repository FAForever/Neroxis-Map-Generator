package com.faforever.neroxis.util.serial.biome;

import com.dslplatform.json.CompiledJson;
import lombok.Data;

@Data
@CompiledJson
public class DecalMaterials {
    private String[] fieldNormals;
    private String[] fieldAlbedos;
    private String[] mountainNormals;
    private String[] mountainAlbedos;
    private String[] slopeNormals;
    private String[] slopeAlbedos;
}
