package com.faforever.neroxis.util.serialized;

import com.dslplatform.json.CompiledJson;
import com.faforever.neroxis.util.Vector2;
import com.faforever.neroxis.util.Vector3;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.faforever.neroxis.map.SCMap.*;

/**
 * Used in disk operations to be converted into a material later
 * Compliant with ozonex's WaterSettings format
 */

@Data
@CompiledJson
public strictfp class WaterSettings {
    private boolean WaterPresent;
    private float Elevation;
    private float ElevationDeep;
    private float ElevationAbyss;

    private Vector3 SurfaceColor;
    private Vector2 ColorLerp;
    private float RefractionScale;
    private float FresnelBias;
    private float FresnelPower;

    private float UnitReflection;
    private float SkyReflection;

    private float SunShininess;
    private float SunStrength;
    private Vector3 SunDirection;
    private Vector3 SunColor;
    private float SunReflection;
    private float SunGlow;

    private String TexPathCubemap;
    private String TexPathWaterRamp;

    private List<WaveTexture> WaveTextures = makeWaveTextures(WAVE_TEXTURE_PATHS, WAVE_NORMAL_REPEATS, WAVE_NORMAL_MOVEMENTS);

    private static List<WaveTexture> makeWaveTextures(String[] WaveTexturePaths, float[] WaveNormalRepeats, Vector2[] WaveNormalMovements) {
        List<WaveTexture> textures = new ArrayList<>();
        for (int i = 0; i < WAVE_NORMAL_COUNT; i++) {
            textures.add(new WaveTexture(WaveTexturePaths[i], WaveNormalMovements[i], WaveNormalRepeats[i]));
        }
        return textures;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static strictfp class WaveTexture {
        private String TexPath;
        private Vector2 NormalMovement;
        private float NormalRepeat;
    }
}