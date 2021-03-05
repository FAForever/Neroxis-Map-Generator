package neroxis.map;

import lombok.Value;
import neroxis.biomes.Biome;

@Value
public class MapParameters {
    int spawnCount;
    float landDensity;
    float plateauDensity;
    float mountainDensity;
    float rampDensity;
    float reclaimDensity;
    int mapSize;
    int numTeams;
    int mexCount;
    int hydroCount;
    boolean unexplored;
    SymmetrySettings symmetrySettings;
    Biome biome;
}
