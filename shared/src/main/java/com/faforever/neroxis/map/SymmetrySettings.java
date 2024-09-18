package com.faforever.neroxis.map;

public record SymmetrySettings(
        Symmetry terrainSymmetry,
        Symmetry teamSymmetry,
        Symmetry spawnSymmetry
) {

    public SymmetrySettings {
        if (terrainSymmetry.getNumSymPoints() % teamSymmetry.getNumSymPoints() != 0) {
            throw new IllegalArgumentException("Team symmetry not a multiple of terrain symmetry");
        }

        if (terrainSymmetry.getNumSymPoints() % spawnSymmetry.getNumSymPoints() != 0) {
            throw new IllegalArgumentException("Spawn symmetry not a multiple of terrain symmetry");
        }

        if (spawnSymmetry.getNumSymPoints() % teamSymmetry.getNumSymPoints() != 0) {
            throw new IllegalArgumentException("Spawn symmetry not a multiple of team symmetry");
        }
    }

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
