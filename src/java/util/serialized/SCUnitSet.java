package util.serialized;

import lombok.Data;
import util.Vector3f;
import util.Vector4f;

import java.util.List;

/**
 * Used in disk operations to be converted into a material later
 */

@Data
public strictfp class SCUnitSet {
    private List<SCUnit> Units;
    private Vector3f Center;

    @Data
    public static strictfp class SCUnit {
        private String ID;
        private Vector3f pos;
        private Vector4f rot;
        private String orders;
        private String platoon;
    }
}