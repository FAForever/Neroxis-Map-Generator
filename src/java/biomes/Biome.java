package biomes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import map.TerrainMaterials;
import util.serialized.WaterSettings;

@AllArgsConstructor
@Getter @Setter
public strictfp class Biome {

    TerrainMaterials terrainMaterials;
    WaterSettings waterSettings;
}
