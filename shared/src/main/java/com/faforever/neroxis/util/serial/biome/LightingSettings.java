package com.faforever.neroxis.util.serial.biome;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;

import java.util.Objects;

/**
 * Used in disk operations to be converted into a material later
 * Compliant with ozonex's SCMLighting format
 */
@CompiledJson
public record LightingSettings(
        @JsonAttribute(mandatory = true, nullable = false) float lightingMultiplier,
        @JsonAttribute(mandatory = true, nullable = false) Vector3 sunDirection,
        @JsonAttribute(mandatory = true, nullable = false) Vector3 sunAmbience,
        @JsonAttribute(mandatory = true, nullable = false) Vector3 sunColor,
        @JsonAttribute(mandatory = true, nullable = false) Vector3 shadowFillColor,
        @JsonAttribute(mandatory = true, nullable = false) Vector4 specularColor,
        @JsonAttribute(mandatory = true, nullable = false) float bloom,
        @JsonAttribute(mandatory = true, nullable = false) Vector3 fogColor,
        @JsonAttribute(mandatory = true, nullable = false) float fogStart,
        @JsonAttribute(mandatory = true, nullable = false) float fogEnd
) {
    public LightingSettings {
        Objects.requireNonNull(sunDirection);
        Objects.requireNonNull(sunAmbience);
        Objects.requireNonNull(sunColor);
        Objects.requireNonNull(shadowFillColor);
        Objects.requireNonNull(specularColor);
        Objects.requireNonNull(fogColor);
    }
}