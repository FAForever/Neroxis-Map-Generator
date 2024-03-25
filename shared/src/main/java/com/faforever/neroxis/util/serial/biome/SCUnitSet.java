package com.faforever.neroxis.util.serial.biome;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;

import java.util.List;
import java.util.Objects;

/**
 * Used in disk operations to be converted into a material later
 */
@CompiledJson
public record SCUnitSet(
        @JsonAttribute(mandatory = true, nullable = false) List<SCUnit> units,
        @JsonAttribute(mandatory = true, nullable = false) Vector3 center
) {
    public SCUnitSet {
        Objects.requireNonNull(center);
        units = List.copyOf(units);
    }

    public record SCUnit(
            @JsonAttribute(mandatory = true, nullable = false) String ID,
            @JsonAttribute(mandatory = true, nullable = false) Vector3 pos,
            @JsonAttribute(mandatory = true, nullable = false) Vector4 rot,
            @JsonAttribute(mandatory = true, nullable = false) String orders,
            @JsonAttribute(mandatory = true, nullable = false) String platoon
    ) {
        public SCUnit {
            Objects.requireNonNull(ID);
            Objects.requireNonNull(pos);
            Objects.requireNonNull(rot);
            Objects.requireNonNull(orders);
            Objects.requireNonNull(platoon);
        }
    }
}