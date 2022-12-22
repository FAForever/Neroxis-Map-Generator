package com.faforever.neroxis.map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SymmetrySettings {
    private Symmetry terrainSymmetry;
    private Symmetry teamSymmetry;
    private Symmetry spawnSymmetry;

    public SymmetrySettings(Symmetry symmetry) {
        terrainSymmetry = symmetry;
        teamSymmetry = symmetry;
        spawnSymmetry = symmetry;
    }

    public Symmetry getSymmetry(SymmetryType symmetryType) {
        return switch (symmetryType) {
            case TEAM -> teamSymmetry;
            case TERRAIN -> terrainSymmetry;
            default -> spawnSymmetry;
        };
    }
}
