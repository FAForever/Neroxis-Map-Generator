package com.faforever.neroxis.util.serial.biome;

import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;
import io.avaje.jsonb.Json;

import java.util.List;
import java.util.Objects;

/**
 * Used in disk operations to be converted into a material later
 */
@Json
public record SCUnitSet(
        @Json.Alias("Units") List<SCUnit> units,
        @Json.Alias("Center") Vector3 center
) {
    public SCUnitSet {
        Objects.requireNonNull(center);
        units = units == null ? List.of() : List.copyOf(units);
    }

    @Json
    public record SCUnit(
            String ID,
            Vector3 pos,
            Vector4 rot,
            String orders,
            String platoon
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