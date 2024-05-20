package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.map.Symmetry;
import lombok.Builder;

@Builder(toBuilder = true)
public record GeneratorParameters(int spawnCount,
                                  int mapSize,
                                  int numTeams,
                                  Visibility visibility,
                                  Symmetry terrainSymmetry,
                                  Biome biome) {

    public String toString() {
        if (visibility == null) {
            return """
                   Spawns: %d
                   Map Size: %d
                   Num Teams: %d
                   Biome: %s
                   Terrain Symmetry: %s
                   """.formatted(spawnCount, mapSize, numTeams, biome.name(), terrainSymmetry);
        } else {
            return """
                   Spawns: %d
                   Map Size: %d
                   Num Teams: %d
                   """.formatted(spawnCount, mapSize, numTeams);
        }
    }
}
