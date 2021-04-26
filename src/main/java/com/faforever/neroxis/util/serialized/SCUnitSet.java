package com.faforever.neroxis.util.serialized;

import com.faforever.neroxis.util.Vector3;
import com.faforever.neroxis.util.Vector4;
import lombok.Data;

import java.util.List;

/**
 * Used in disk operations to be converted into a material later
 */

@Data
public strictfp class SCUnitSet {
    private List<SCUnit> Units;
    private Vector3 Center;

    @Data
    public static strictfp class SCUnit {
        private String ID;
        private Vector3 pos;
        private Vector4 rot;
        private String orders;
        private String platoon;
    }
}