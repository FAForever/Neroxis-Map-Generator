package com.faforever.neroxis.util.serialized;

import com.dslplatform.json.CompiledJson;
import com.faforever.neroxis.util.Vector3;
import com.faforever.neroxis.util.Vector4;
import lombok.Data;
import lombok.Value;

/**
 * Used in disk operations to be converted into a material later
 */

@Data
@CompiledJson
public strictfp class SCUnitSet {
    public SCUnit[] Units;
    public Vector3 Center;

    @Value
    public static strictfp class SCUnit {
        String ID;
        Vector3 pos;
        Vector4 rot;
        String orders;
        String platoon;
    }
}