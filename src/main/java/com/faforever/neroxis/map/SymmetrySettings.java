package com.faforever.neroxis.map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public strictfp class SymmetrySettings {
    private Symmetry terrainSymmetry;
    private Symmetry teamSymmetry;
    private Symmetry spawnSymmetry;

    public SymmetrySettings(Symmetry symmetry) {
        terrainSymmetry = symmetry;
        teamSymmetry = symmetry;
        spawnSymmetry = symmetry;
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
