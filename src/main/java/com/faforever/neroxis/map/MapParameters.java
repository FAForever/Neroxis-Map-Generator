package com.faforever.neroxis.map;

import com.faforever.neroxis.biomes.Biome;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public strictfp class MapParameters {
    int spawnCount;
    float landDensity;
    float plateauDensity;
    float mountainDensity;
    float rampDensity;
    float reclaimDensity;
    float mexDensity;
    int mapSize;
    int numTeams;
    int hydroCount;
    boolean unexplored;
    boolean blind;
    boolean tournamentStyle;
    SymmetrySettings symmetrySettings;
    Biome biome;

    public String toString() {
        if (!tournamentStyle) {
            return "Spawns: " + spawnCount +
                    "\nMap Size: " + mapSize +
                    "\nNum Teams: " + numTeams +
                    "\nBiome: " + biome.getName() +
                    "\nLand Density: " + landDensity +
                    "\nPlateau Density: " + plateauDensity +
                    "\nMountain Density: " + mountainDensity +
                    "\nRamp Density: " + rampDensity +
                    "\nReclaim Density: " + reclaimDensity +
                    "\nMex Density: " + mexDensity +
                    "\nTerrain Symmetry: " + symmetrySettings.getTerrainSymmetry() +
                    "\nTeam Symmetry: " + symmetrySettings.getTeamSymmetry() +
                    "\nSpawn Symmetry: " + symmetrySettings.getSpawnSymmetry();
        } else {
            return "Spawns: " + spawnCount +
                    "\nMap Size: " + mapSize +
                    "\nNum Teams: " + numTeams;
        }
    }
}
