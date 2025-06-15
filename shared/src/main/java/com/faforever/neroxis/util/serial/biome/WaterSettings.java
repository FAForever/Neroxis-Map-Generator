package com.faforever.neroxis.util.serial.biome;

import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;
import io.avaje.jsonb.Json;

import java.util.List;
import java.util.Objects;

import static com.faforever.neroxis.map.SCMap.WAVE_NORMAL_COUNT;

/**
 * Used in disk operations to be converted into a material later
 * Compliant with ozonex's WaterSettings format
 */
@Json
public record WaterSettings(
        boolean waterPresent,
        float elevation,
        float elevationDeep,
        float elevationAbyss,
        Vector3 surfaceColor,
        Vector2 colorLerp,
        float refractionScale,
        float fresnelBias,
        float fresnelPower,
        float unitReflection,
        float skyReflection,
        float sunShininess,
        float sunStrength,
        Vector3 sunDirection,
        Vector3 sunColor,
        float sunReflection,
        float sunGlow,
        String texPathCubemap,
        String texPathWaterRamp,
        List<WaveTexture> waveTextures
) {

    public WaterSettings {
        Objects.requireNonNull(surfaceColor);
        Objects.requireNonNull(colorLerp);
        Objects.requireNonNull(sunDirection);
        Objects.requireNonNull(sunColor);
        Objects.requireNonNull(texPathCubemap);
        Objects.requireNonNull(texPathWaterRamp);
        waveTextures = List.copyOf(waveTextures);

        if (waveTextures.size() != WAVE_NORMAL_COUNT) {
            throw new IllegalArgumentException("Number of wave textures must be 4");
        }
    }

    @Json
    public record WaveTexture(
            Vector2 normalMovement,
            String texPath,
            float normalRepeat
    ) {
        public WaveTexture {
            Objects.requireNonNull(normalMovement);
            Objects.requireNonNull(texPath);
        }
    }
}