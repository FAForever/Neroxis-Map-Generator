package com.faforever.neroxis.util.serial.biome;

import com.faforever.neroxis.util.vector.Vector3;
import com.faforever.neroxis.util.vector.Vector4;
import io.avaje.jsonb.Json;

import java.util.Objects;

/**
 * Used in disk operations to be converted into a material later
 * Compliant with ozonex's SCMLighting format
 */
@Json
public record LightingSettings(
        float lightingMultiplier,
        Vector3 sunDirection,
        Vector3 sunAmbience,
        Vector3 sunColor,
        Vector3 shadowFillColor,
        Vector4 specularColor,
        float bloom,
        Vector3 fogColor,
        float fogStart,
        float fogEnd
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