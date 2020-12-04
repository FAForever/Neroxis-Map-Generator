package map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SymmetrySettings {
    private Symmetry TerrainSymmetry;
    private Symmetry TeamSymmetry;
    private Symmetry SpawnSymmetry;
}
