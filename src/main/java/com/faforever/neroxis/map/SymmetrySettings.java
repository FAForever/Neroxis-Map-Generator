package com.faforever.neroxis.map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public strictfp class SymmetrySettings {
    private Symmetry terrainSymmetry;
    private Symmetry teamSymmetry;
    private Symmetry spawnSymmetry;

    public SymmetrySettings() {
        terrainSymmetry = Symmetry.NONE;
        teamSymmetry = Symmetry.NONE;
        spawnSymmetry = Symmetry.NONE;
    }

    public Symmetry getSymmetry(SymmetryType symmetryType) {
        switch (symmetryType) {
            case TEAM:
                return teamSymmetry;
            case TERRAIN:
                return terrainSymmetry;
            default:
                return spawnSymmetry;
        }
    }
}
