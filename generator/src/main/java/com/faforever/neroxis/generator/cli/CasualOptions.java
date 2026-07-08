package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.map.Symmetry;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

@Getter
public class CasualOptions {
    @CommandLine.ArgGroup
    private @Nullable StyleOptions styleOptions;
    @CommandLine.Option(names = "--seed", order = 3, description = "Seed for the generated map")
    private @Nullable Long seed;
    @CommandLine.Option(
            names = "--terrain-symmetry",
            order = 100,
            description = "Base terrain symmetry for the generated map. Values: ${COMPLETION-CANDIDATES}"
    )
    private @Nullable Symmetry terrainSymmetry;
}
