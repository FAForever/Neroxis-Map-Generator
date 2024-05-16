package com.faforever.neroxis.generator;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.map.Symmetry;
import lombok.Builder;

@Builder(toBuilder = true)
public record GeneratorParameters(int spawnCount,
                                  float landDensity,
                                  float plateauDensity,
                                  float mountainDensity,
                                  float rampDensity,
                                  float reclaimDensity,
                                  float mexDensity,
                                  int mapSize,
                                  int numTeams,
                                  ResourceGenerator resourceGenerator,
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
                   Land Density: %s
                   Plateau Density: %s
                   Mountain Density: %s
                   Ramp Density: %s
                   Reclaim Density: %s
                   Mex Density: %s
                   Terrain Symmetry: %s
                   """.formatted(spawnCount, mapSize, numTeams, biome.name(), landDensity, plateauDensity,
                                 mountainDensity, rampDensity, reclaimDensity, mexDensity, terrainSymmetry);
        } else {
            return """
                   Spawns: %d
                   Map Size: %d
                   Num Teams: %d
                   """.formatted(spawnCount, mapSize, numTeams);
        }
    }
}
