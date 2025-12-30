import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;

void main() {
    for (int i = 0; i < 10; i++) {
        BooleanMask outsideMask = new BooleanMask(1024, 0L, new SymmetrySettings(Symmetry.NONE));
        outsideMask.randomize(.1f);
        outsideMask.copy().inflate(25);
    }
}
