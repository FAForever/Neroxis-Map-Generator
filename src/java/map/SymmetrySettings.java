package map;

import lombok.Data;

@Data
public class SymmetrySettings {
    private final Symmetry TerrainSymmetry;
    private final Symmetry TeamSymmetry;
    private Symmetry SpawnSymmetry;
}
