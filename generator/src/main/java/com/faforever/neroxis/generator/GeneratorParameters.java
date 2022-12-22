package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.map.Symmetry;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class GeneratorParameters {
    int spawnCount;
    float landDensity;
    float plateauDensity;
    float mountainDensity;
    float rampDensity;
    float reclaimDensity;
    float mexDensity;
    int mapSize;
    int numTeams;
    Visibility visibility;
    Symmetry terrainSymmetry;
    Biome biome;

    public String toString() {
        if (visibility == null) {
            return "Spawns: "
                   + spawnCount
                   + "\nMap Size: "
                   + mapSize
                   + "\nNum Teams: "
                   + numTeams
                   + "\nBiome: "
                   + biome.getName()
                   + "\nLand Density: "
                   + landDensity
                   + "\nPlateau Density: "
                   + plateauDensity
                   + "\nMountain Density: "
                   + mountainDensity
                   + "\nRamp Density: "
                   + rampDensity
                   + "\nReclaim Density: "
                   + reclaimDensity
                   + "\nMex Density: "
                   + mexDensity
                   + "\nTerrain Symmetry: "
                   + terrainSymmetry;
        } else {
            return "Spawns: " + spawnCount + "\nMap Size: " + mapSize + "\nNum Teams: " + numTeams;
        }
    }
}
