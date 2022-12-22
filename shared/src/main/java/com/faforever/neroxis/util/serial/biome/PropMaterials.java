package com.faforever.neroxis.util.serial.biome;

import com.dslplatform.json.CompiledJson;
import lombok.Data;

@Data
@CompiledJson
public class PropMaterials {
    private String[] treeGroups;
    private String[] rocks;
    private String[] boulders;
}
