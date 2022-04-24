package com.faforever.neroxis.map;

import com.dslplatform.json.CompiledJson;
import lombok.Data;

@Data
@CompiledJson
public strictfp class PropMaterials {

    private String[] treeGroups;
    private String[] rocks;
    private String[] boulders;
}
