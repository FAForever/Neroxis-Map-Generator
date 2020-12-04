package map;

import lombok.AllArgsConstructor;
import lombok.Data;
import util.Vector2f;

@Data
@AllArgsConstructor
public strictfp class SymmetryPoint {
    Vector2f location;
    Symmetry symmetry;
}