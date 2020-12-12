package map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SymmetrySettings {
    private Symmetry terrainSymmetry;
    private Symmetry teamSymmetry;
    private Symmetry spawnSymmetry;

    public Symmetry getSymmetry(SymmetryType symmetryType) {
        return switch (symmetryType) {
            case TEAM -> teamSymmetry;
            case SPAWN -> spawnSymmetry;
            case TERRAIN -> terrainSymmetry;
        };
    }
}
