package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.map.Symmetry;
import lombok.Getter;
import picocli.CommandLine;

@Getter
public class CasualOptions {
    @CommandLine.ArgGroup
    private StyleOptions styleOptions = new StyleOptions();
    private Symmetry terrainSymmetry;

    @CommandLine.Option(names = "--terrain-symmetry", order = 100, description = "Base terrain symmetry for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setTerrainSymmetry(Symmetry terrainSymmetry) {
        this.terrainSymmetry = terrainSymmetry;
    }
}
