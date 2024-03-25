package com.faforever.neroxis.map;

public record SymmetrySettings(
        Symmetry terrainSymmetry,
        Symmetry teamSymmetry,
        Symmetry spawnSymmetry
) {
    public SymmetrySettings(Symmetry symmetry) {
        this(symmetry, symmetry, symmetry);
    }

    public Symmetry getSymmetry(SymmetryType symmetryType) {
        return switch (symmetryType) {
            case TEAM -> teamSymmetry;
            case TERRAIN -> terrainSymmetry;
            case SPAWN -> spawnSymmetry;
        };
    }
}
