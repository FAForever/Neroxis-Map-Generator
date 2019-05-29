package util.serialized;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import util.Vector2f;
import util.Vector3f;

import java.util.LinkedList;
import java.util.List;

import static map.SCMap.*;

/** Used in disk operations to be converted into a material later
 * Compliant with ozonex's WaterSettings format
 * */

public strictfp class WaterSettings {
    public boolean HasWater = true;
    public float Elevation = WATER_HEIGHT;
    public float ElevationDeep = WATER_DEEP_HEIGHT;
    public float ElevationAbyss = WATER_ABYSS_HEIGHT;


    public Vector3f SurfaceColor = WATER_SURFACE_COLOR;
    public Vector2f ColorLerp = WATER_COLOR_LERP;
    public float RefractionScale = WATER_REFRACTION;
    public float FresnelBias = WATER_FRESNEL_BIAS;
    public float FresnelPower = WATER_FRESNEL_POWER;

    public float UnitReflection = WATER_UNIT_REFLECTION;
    public float SkyReflection = WATER_SKY_REFLECTION;

    public float SunShininess = WATER_SUN_SHININESS;
    public float SunStrength = WATER_SUN_STRENGH;
    public Vector3f SunDirection = WATER_SUN_DIRECTION;
    public Vector3f SunColor = WATER_SUN_COLOR;
    public float SunReflection = WATER_SUN_REFLECTION;
    public float SunGlow = WATER_SUN_GLOW;

    public String TexPathCubemap = WATER_CUBEMAP_PATH;
    public String TexPathWaterRamp = WATER_RAMP_PATH;

    public WaveTexture[] WaveTextures = makeWaveTextures(WAVE_TEXTURE_PATHS, WAVE_NORMAL_REPEATS, WAVE_NORMAL_MOVEMENTS);

    private static WaveTexture[] makeWaveTextures(
            String[] waveTexturePaths,
            float[] waveNormalRepeats,
            Vector2f[] waveNormalMovements){
        List<WaveTexture> texs = new LinkedList<>();
        for (int i = 0; i < WAVE_NORMAL_COUNT; i++){
            texs.add( new WaveTexture(waveTexturePaths[i], waveNormalMovements[i], waveNormalRepeats[i] ));
        }
        WaveTexture[] finalArray = new WaveTexture[texs.size()];
        texs.toArray(finalArray);
        return finalArray;
    }
    
    @AllArgsConstructor
    public static class WaveTexture{
        public String TexPath;
        public Vector2f NormalMovement;
        public float NormalRepeat;
    }
}