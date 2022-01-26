package com.faforever.neroxis.biomes;

import com.faforever.neroxis.map.DecalMaterials;
import com.faforever.neroxis.map.PropMaterials;
import com.faforever.neroxis.map.TerrainMaterials;
import com.faforever.neroxis.util.serial.LightingSettings;
import com.faforever.neroxis.util.serial.WaterSettings;
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
