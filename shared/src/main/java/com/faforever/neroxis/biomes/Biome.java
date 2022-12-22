package com.faforever.neroxis.biomes;

import com.faforever.neroxis.util.serial.biome.*;
import lombok.Data;

@Data
public class Biome {
    final String name;
    final TerrainMaterials terrainMaterials;
    final PropMaterials propMaterials;
    final DecalMaterials decalMaterials;
    final WaterSettings waterSettings;
    final LightingSettings lightingSettings;
}
