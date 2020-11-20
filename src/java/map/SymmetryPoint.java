package map;

import lombok.Value;
import util.Vector2f;

@Value
public strictfp class SymmetryPoint {
    Vector2f location;
    Symmetry symmetry;
}