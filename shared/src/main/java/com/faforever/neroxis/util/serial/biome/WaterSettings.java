package com.faforever.neroxis.util.serial.biome;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

import java.util.List;
import java.util.Objects;

import static com.faforever.neroxis.map.SCMap.WAVE_NORMAL_COUNT;

/**
 * Used in disk operations to be converted into a material later
 * Compliant with ozonex's WaterSettings format
 */
@CompiledJson
public record WaterSettings(
        @JsonAttribute(mandatory = true, nullable = false) boolean waterPresent,
        @JsonAttribute(mandatory = true, nullable = false) float elevation,
        @JsonAttribute(mandatory = true, nullable = false) float elevationDeep,
        @JsonAttribute(mandatory = true, nullable = false) float elevationAbyss,
        @JsonAttribute(mandatory = true, nullable = false) Vector3 surfaceColor,
        @JsonAttribute(mandatory = true, nullable = false) Vector2 colorLerp,
        @JsonAttribute(mandatory = true, nullable = false) float refractionScale,
        @JsonAttribute(mandatory = true, nullable = false) float fresnelBias,
        @JsonAttribute(mandatory = true, nullable = false) float fresnelPower,
        @JsonAttribute(mandatory = true, nullable = false) float unitReflection,
        @JsonAttribute(mandatory = true, nullable = false) float skyReflection,
        @JsonAttribute(mandatory = true, nullable = false) float sunShininess,
        @JsonAttribute(mandatory = true, nullable = false) float sunStrength,
        @JsonAttribute(mandatory = true, nullable = false) Vector3 sunDirection,
        @JsonAttribute(mandatory = true, nullable = false) Vector3 sunColor,
        @JsonAttribute(mandatory = true, nullable = false) float sunReflection,
        @JsonAttribute(mandatory = true, nullable = false) float sunGlow,
        @JsonAttribute(mandatory = true, nullable = false) String texPathCubemap,
        @JsonAttribute(mandatory = true, nullable = false) String texPathWaterRamp,
        @JsonAttribute(mandatory = true, nullable = false) List<WaveTexture> waveTextures
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

    public record WaveTexture(
            @JsonAttribute(mandatory = true, nullable = false) Vector2 normalMovement,
            @JsonAttribute(mandatory = true, nullable = false) String texPath,
            @JsonAttribute(mandatory = true, nullable = false) float normalRepeat
    ) {
        public WaveTexture {
            Objects.requireNonNull(normalMovement);
            Objects.requireNonNull(texPath);
        }
    }
}