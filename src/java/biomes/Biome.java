package biomes;

import map.TerrainMaterials;
import util.serialized.WaterSettings;

public strictfp class Biome {
    public TerrainMaterials terrainMaterials;
    public WaterSettings waterSettings;

    public Biome(TerrainMaterials terrainMaterials, WaterSettings waterSettings){
        this.terrainMaterials = terrainMaterials;
        this.waterSettings = waterSettings;
    }
}
