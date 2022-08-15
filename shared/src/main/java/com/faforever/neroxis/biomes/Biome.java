package com.faforever.neroxis.biomes;

import com.faforever.neroxis.util.serial.biome.DecalMaterials;
import com.faforever.neroxis.util.serial.biome.LightingSettings;
import com.faforever.neroxis.util.serial.biome.PropMaterials;
import com.faforever.neroxis.util.serial.biome.TerrainMaterials;
import com.faforever.neroxis.util.serial.biome.WaterSettings;
import lombok.Data;

@Data
public strictfp class Biome {
    final String name;
    final TerrainMaterials terrainMaterials;
    final PropMaterials propMaterials;
    final DecalMaterials decalMaterials;
    final WaterSettings waterSettings;
    final LightingSettings lightingSettings;
}
