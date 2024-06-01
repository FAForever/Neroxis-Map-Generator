package com.faforever.neroxis.generator;

import com.faforever.neroxis.map.Symmetry;
import lombok.Builder;

@Builder(toBuilder = true)
public record GeneratorParameters(int spawnCount,
                                  int mapSize,
                                  int numTeams,
                                  Visibility visibility,
                                  Symmetry terrainSymmetry) {

    public String toString() {
        if (visibility == null) {
            return """
                   Spawns: %d
                   Map Size: %d
                   Num Teams: %d
                   Terrain Symmetry: %s
                   """.formatted(spawnCount, mapSize, numTeams, terrainSymmetry);
        } else {
            return """
                   Spawns: %d
                   Map Size: %d
                   Num Teams: %d
                   """.formatted(spawnCount, mapSize, numTeams);
        }
    }
}
