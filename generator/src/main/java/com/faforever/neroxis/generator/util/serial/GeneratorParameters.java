package com.faforever.neroxis.generator.util.serial;

import com.faforever.neroxis.map.Symmetry;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.SplittableRandom;

public record GeneratorParameters(
        long seed,
        int spawnCount,
        int mapSize,
        int numTeams,
        Mode mode
) {

    public GeneratorParameters {
        if (numTeams != 0 && spawnCount % numTeams != 0) {
            throw new IllegalArgumentException(
                    "Spawn Count `%d` not a multiple of Num Teams `%d`".formatted(spawnCount, numTeams));
        }

        if (numTeams != 0 &&
            mode instanceof GeneratorParameters.Casual casual &&
            casual.terrainSymmetry() != null &&
            casual.terrainSymmetry().getNumSymPoints() % numTeams != 0) {
            throw new IllegalArgumentException(
                    "Terrain symmetry `%s` not compatible with Num Teams `%d`".formatted(casual.terrainSymmetry(),
                                                                                         numTeams));
        }
    }

    public boolean allowDebug() {
        return mode instanceof Casual;
    }

    public boolean canPlaceUnits() {
        return !(mode instanceof Competitive(_, Visibility visibility) && visibility == Visibility.UNEXPLORED);
    }

    public SplittableRandom createRandom() {
        if (mode instanceof Competitive(long generationTime, _)) {
            return new SplittableRandom(
                    new SplittableRandom(seed()).nextLong() ^ new SplittableRandom(generationTime).nextLong());
        } else {
            return new SplittableRandom(seed());
        }
    }

    @Override
    public String toString() {
        return """
               Seed: %d
               Spawns: %d
               Map Size: %d
               Num Teams: %d
               %s""".formatted(seed, spawnCount, mapSize, numTeams, mode);
    }

    public sealed interface Mode {}

    public record Competitive(
            long generationTime,
            Visibility visibility
    ) implements Mode {
        public Competitive {
            if (generationTime == 0) {
                throw new IllegalArgumentException("Generation Time must exist for competitive mode");
            }
        }

        @Override
        public String toString() {
            return """
                   Generation Time: %s
                   Visibility: %s""".formatted(generationTime == 0 ? null : Instant.ofEpochSecond(generationTime),
                                               visibility);
        }
    }

    public record Casual(
            @Nullable
            Symmetry terrainSymmetry,
            @Nullable
            MapStyle mapStyle
    ) implements Mode {
        @Override
        public String toString() {
            return """
                   Terrain Symmetry: %s
                   Style: %s""".formatted(terrainSymmetry, mapStyle);
        }
    }
}
