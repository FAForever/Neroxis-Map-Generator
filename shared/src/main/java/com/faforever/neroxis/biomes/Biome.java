package com.faforever.neroxis.biomes;

import com.faforever.neroxis.util.serial.biome.DecalMaterials;
import com.faforever.neroxis.util.serial.biome.LightingSettings;
import com.faforever.neroxis.util.serial.biome.PropMaterials;
import com.faforever.neroxis.util.serial.biome.TerrainMaterials;
import com.faforever.neroxis.util.serial.biome.WaterSettings;

import java.util.Objects;

public record Biome(
        BiomeName name,
        TerrainMaterials terrainMaterials,
        PropMaterials propMaterials,
        DecalMaterials decalMaterials,
        WaterSettings waterSettings,
        LightingSettings lightingSettings
) {
    public Biome {
        Objects.requireNonNull(terrainMaterials);
        Objects.requireNonNull(propMaterials);
        Objects.requireNonNull(decalMaterials);
        Objects.requireNonNull(waterSettings);
        Objects.requireNonNull(lightingSettings);
    }
}
