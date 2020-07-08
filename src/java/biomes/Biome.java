package biomes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import map.TerrainMaterials;
import util.serialized.LightingSettings;
import util.serialized.WaterSettings;

@AllArgsConstructor
@Getter
@Setter
public strictfp class Biome {
    String name;
    TerrainMaterials terrainMaterials;
    WaterSettings waterSettings;
    LightingSettings lightingSettings;
}
