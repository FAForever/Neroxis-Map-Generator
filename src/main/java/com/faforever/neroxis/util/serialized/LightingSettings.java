package com.faforever.neroxis.util.serialized;

import com.dslplatform.json.CompiledJson;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;
import lombok.Data;

/**
 * Used in disk operations to be converted into a material later
 * Compliant with ozonex's SCMLighting format
 */

@Data
@CompiledJson
public strictfp class LightingSettings {
    private float LightingMultiplier;
    private Vector3 SunDirection;
    private Vector3 SunAmbience;
    private Vector3 SunColor;
    private Vector3 ShadowFillColor;
    private Vector4 SpecularColor;
    private float Bloom;
    private Vector3 FogColor;
    private float FogStart;
    private float FogEnd;

}