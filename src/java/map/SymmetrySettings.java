package map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SymmetrySettings {
    private final Symmetry TerrainSymmetry;
    private final Symmetry TeamSymmetry;
    private Symmetry SpawnSymmetry;
}
