package biomes;

import lombok.Data;
import map.PropMaterials;
import map.TerrainMaterials;
import util.serialized.LightingSettings;
import util.serialized.WaterSettings;

@Data
public strictfp class Biome {
    final String name;
    final TerrainMaterials terrainMaterials;
    final PropMaterials propMaterials;
    final WaterSettings waterSettings;
    final LightingSettings lightingSettings;
}
