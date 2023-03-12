package com.faforever.neroxis.util.serial.biome;

import com.dslplatform.json.CompiledJson;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;

/**
 * Used in disk operations to be converted into a material later
 */
@CompiledJson
public record SCUnitSet(SCUnit[] Units, Vector3 Center) {
    public record SCUnit(String ID, Vector3 pos, Vector4 rot, String orders, String platoon) {}
}