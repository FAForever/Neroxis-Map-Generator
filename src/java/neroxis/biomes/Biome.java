package neroxis.biomes;

import lombok.Data;
import neroxis.map.PropMaterials;
import neroxis.map.TerrainMaterials;
import neroxis.util.serialized.LightingSettings;
import neroxis.util.serialized.WaterSettings;

@Data
public strictfp class Biome {
    final String name;
    final TerrainMaterials terrainMaterials;
    final PropMaterials propMaterials;
    final WaterSettings waterSettings;
    final LightingSettings lightingSettings;
}
