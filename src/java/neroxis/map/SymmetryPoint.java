package neroxis.map;

import lombok.AllArgsConstructor;
import lombok.Data;
import neroxis.util.Vector2f;

@Data
@AllArgsConstructor
public strictfp class SymmetryPoint {
    Vector2f location;
    Symmetry symmetry;
}