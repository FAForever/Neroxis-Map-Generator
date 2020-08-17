package util.serialized;

import lombok.Data;
import util.Vector3f;
import util.Vector4f;

import static map.SCMap.*;

/**
 * Used in disk operations to be converted into a material later
 * Compliant with ozonex's SCMLighting format
 */

@Data
public strictfp class LightingSettings {
    private float LightingMultiplier = LIGHTING_MULTIPLIER;
    private Vector3f SunDirection = SUN_DIRECTION;
    private Vector3f SunAmbience = SUN_AMBIANCE_COLOR;
    private Vector3f SunColor = SUN_COLOR;
    private Vector3f ShadowFillColor = SHADOW_COLOR;
    private Vector4f SpecularColor = SPECULAR_COLOR;
    private float Bloom = BLOOM;
    private Vector3f FogColor = FOG_COLOR;
    private float FogStart = FOG_START;
    private float FogEnd = FOG_END;

}