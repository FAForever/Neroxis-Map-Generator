package biomes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import map.TerrainMaterials;
import util.serialized.WaterSettings;

@AllArgsConstructor
public strictfp class Biome {

    @Getter @Setter
    TerrainMaterials terrainMaterials;

    @Getter @Setter
    WaterSettings waterSettings;
}
