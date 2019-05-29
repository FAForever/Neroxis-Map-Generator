package biomes;

import lombok.AllArgsConstructor;
import map.TerrainMaterials;
import util.serialized.WaterSettings;

@AllArgsConstructor
public strictfp class Biome {
    public TerrainMaterials terrainMaterials;
    public WaterSettings waterSettings;
}
