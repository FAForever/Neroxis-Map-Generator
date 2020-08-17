package util.serialized;

import lombok.AllArgsConstructor;
import lombok.Data;
import util.Vector2f;
import util.Vector3f;

import java.util.LinkedList;
import java.util.List;

import static map.SCMap.*;

/**
 * Used in disk operations to be converted into a material later
 * Compliant with ozonex's WaterSettings format
 */

@Data
public strictfp class WaterSettings {
    private boolean waterPresent = true;
    private float elevation = WATER_HEIGHT;
    private float elevationDeep = WATER_DEEP_HEIGHT;
    private float elevationAbyss = WATER_ABYSS_HEIGHT;

    private Vector3f surfaceColor = WATER_SURFACE_COLOR;
    private Vector2f colorLerp = WATER_COLOR_LERP;
    private float refractionScale = WATER_REFRACTION;
    private float fresnelBias = WATER_FRESNEL_BIAS;
    private float fresnelPower = WATER_FRESNEL_POWER;

    private float unitReflection = WATER_UNIT_REFLECTION;
    private float skyReflection = WATER_SKY_REFLECTION;

    private float sunShininess = WATER_SUN_SHININESS;
    private float sunStrength = WATER_SUN_STRENGH;
    private Vector3f sunDirection = WATER_SUN_DIRECTION;
    private Vector3f sunColor = WATER_SUN_COLOR;
    private float sunReflection = WATER_SUN_REFLECTION;
    private float sunGlow = WATER_SUN_GLOW;

    private String texPathCubemap = WATER_CUBEMAP_PATH;
    private String texPathWaterRamp = WATER_RAMP_PATH;

    private WaveTexture[] waveTextures = makeWaveTextures(WAVE_TEXTURE_PATHS, WAVE_NORMAL_REPEATS, WAVE_NORMAL_MOVEMENTS);

    private static WaveTexture[] makeWaveTextures(
            String[] waveTexturePaths,
            float[] waveNormalRepeats,
            Vector2f[] waveNormalMovements) {
        List<WaveTexture> texs = new LinkedList<>();
        for (int i = 0; i < WAVE_NORMAL_COUNT; i++) {
            texs.add(new WaveTexture(waveTexturePaths[i], waveNormalMovements[i], waveNormalRepeats[i]));
        }
        return texs.toArray(new WaveTexture[0]);
    }

    @Data
    @AllArgsConstructor
    public static strictfp class WaveTexture {
        private String TexPath;
        private Vector2f NormalMovement;
        private float NormalRepeat;
    }
}