package util.serialized;

import com.google.gson.annotations.SerializedName;
import util.Vector2f;
import util.Vector3f;

import java.util.LinkedList;
import java.util.List;

import static map.SCMap.*;

// Used in disk operations
// Compliant with ozonex's WaterSettings format
public strictfp class WaterSettings {
    public boolean HasWater;
    public float Elevation;
    public float ElevationDeep;
    public float ElevationAbyss;


    public Vector3f SurfaceColor;
    public Vector2f ColorLerp;
    public float RefractionScale;
    public float FresnelBias;
    public float FresnelPower;

    public float UnitReflection;
    public float SkyReflection;

    public float SunShininess;
    public float SunStrength;
    public Vector3f SunDirection;
    public Vector3f SunColor;
    public float SunReflection;
    public float SunGlow;

    public String TexPathCubemap;
    public String TexPathWaterRamp;

    public WaveTexture[] WaveTextures;

    public WaterSettings(){
        HasWater = true;
        Elevation = WATER_HEIGHT;
        ElevationDeep = WATER_DEEP_HEIGHT;
        ElevationAbyss = WATER_ABYSS_HEIGHT;
        SurfaceColor = WATER_SURFACE_COLOR;
        ColorLerp = WATER_COLOR_LERP;
        RefractionScale = WATER_REFRACTION;
        FresnelBias = WATER_FRESNEL_BIAS;
        FresnelPower = WATER_FRESNEL_POWER;
        UnitReflection = WATER_UNIT_REFLECTION;
        SkyReflection = WATER_SKY_REFLECTION;
        SunShininess = WATER_SUN_SHININESS;
        SunStrength = WATER_SUN_STRENGH;
        SunDirection = WATER_SUN_DIRECTION;
        SunColor = WATER_SUN_COLOR;
        SunReflection = WATER_SUN_REFLECTION;
        SunGlow = WATER_SUN_GLOW;
        TexPathCubemap = WATER_CUBEMAP_PATH;
        TexPathWaterRamp = WATER_RAMP_PATH;
        WaveTextures = makeWaveTextures(WAVE_TEXTURE_PATHS, WAVE_NORMAL_REPEATS, WAVE_NORMAL_MOVEMENTS);
    }

    private static WaveTexture[] makeWaveTextures(
            String[] waveTexturePaths,
            float[] waveNormalRepeats,
            Vector2f[] waveNormalMovements){
        List<WaveTexture> texs = new LinkedList<>();
        for (int i = 0; i < WAVE_NORMAL_COUNT; i++){
            texs.add(
                new WaveTexture(
                    waveTexturePaths[i],
                    waveNormalMovements[i],
                    waveNormalRepeats[i]
                )
            );
        }
        WaveTexture[] finalArray = new WaveTexture[texs.size()];
        texs.toArray(finalArray);
        return finalArray;
    }

    public static class WaveTexture{
        public String TexPath;
        public Vector2f NormalMovement;
        public float NormalRepeat;

        WaveTexture(String texPath, Vector2f normalMovement, float normalRepeat){
            this.TexPath = texPath;
            this.NormalMovement = normalMovement;
            this.NormalRepeat = normalRepeat;
        }
    }
}