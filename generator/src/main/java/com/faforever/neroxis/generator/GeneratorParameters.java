package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.util.serial.MapStyle;
import com.faforever.neroxis.generator.util.serial.Visibility;
import com.faforever.neroxis.map.SymmetrySettings;
import org.jspecify.annotations.Nullable;

public record GeneratorParameters(
        int spawnCount,
        int mapSize,
        int numTeams,
        MapStyle mapStyle,
        SymmetrySettings symmetrySettings,
        @Nullable
        Visibility visibility
) implements SizeSpawnParameters {

    public GeneratorParameters {
        if (numTeams != 0 && spawnCount % numTeams != 0) {
            throw new IllegalArgumentException(
                    "Spawn Count `%d` not a multiple of Num Teams `%d`".formatted(spawnCount, numTeams));
        }

        if (numTeams != 0 && symmetrySettings.terrainSymmetry().getNumSymPoints() % numTeams != 0) {
            throw new IllegalArgumentException(
                    "Terrain symmetry `%s` not compatible with Num Teams `%d`".formatted(
                            symmetrySettings.terrainSymmetry(),
                            numTeams));
        }
    }
}
