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
    private boolean WaterPresent;
    private float Elevation;
    private float ElevationDeep;
    private float ElevationAbyss;

    private Vector3f SurfaceColor;
    private Vector2f ColorLerp;
    private float RefractionScale;
    private float FresnelBias;
    private float FresnelPower;

    private float UnitReflection;
    private float SkyReflection;

    private float SunShininess;
    private float SunStrength;
    private Vector3f SunDirection;
    private Vector3f SunColor;
    private float SunReflection;
    private float SunGlow;

    private String TexPathCubemap;
    private String TexPathWaterRamp;

    private WaveTexture[] WaveTextures = makeWaveTextures(WAVE_TEXTURE_PATHS, WAVE_NORMAL_REPEATS, WAVE_NORMAL_MOVEMENTS);

    private static WaveTexture[] makeWaveTextures(
            String[] WaveTexturePaths,
            float[] WaveNormalRepeats,
            Vector2f[] WaveNormalMovements) {
        List<WaveTexture> textures = new LinkedList<>();
        for (int i = 0; i < WAVE_NORMAL_COUNT; i++) {
            textures.add(new WaveTexture(WaveTexturePaths[i], WaveNormalMovements[i], WaveNormalRepeats[i]));
        }
        return textures.toArray(new WaveTexture[0]);
    }

    @Data
    @AllArgsConstructor
    public static strictfp class WaveTexture {
        private String TexPath;
        private Vector2f NormalMovement;
        private float NormalRepeat;
    }
}