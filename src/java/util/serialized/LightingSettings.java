package util.serialized;

import util.Vector3f;
import util.Vector4f;

import static map.SCMap.*;

/**
 * Used in disk operations to be converted into a material later
 * Compliant with ozonex's SCMLighting format
 */

public strictfp class LightingSettings {
    public float LightingMultiplier = LIGHTING_MULTIPLIER;
    public Vector3f SunDirection = SUN_DIRECTION;
    public Vector3f SunAmbience = SUN_AMBIANCE_COLOR;
    public Vector3f SunColor = SUN_COLOR;
    public Vector3f ShadowFillColor = SHADOW_COLOR;
    public Vector4f SpecularColor = SPECULAR_COLOR;
    public float Bloom = BLOOM;
    public Vector3f FogColor = FOG_COLOR;
    public float FogStart = FOG_START;
    public float FogEnd = FOG_END;

}