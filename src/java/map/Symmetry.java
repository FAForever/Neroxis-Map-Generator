package map;

import lombok.Getter;

@Getter
public strictfp enum Symmetry {
    POINT2(true, 2),
    POINT3(false, 3),
    POINT4(true, 4),
    POINT5(false, 5),
    POINT6(false, 6),
    POINT7(false, 7),
    POINT8(false, 8),
    POINT9(false, 9),
    POINT10(false, 10),
    POINT11(false, 11),
    POINT12(false, 12),
    POINT13(false, 13),
    POINT14(false, 14),
    POINT15(false, 15),
    POINT16(false, 16),
    XZ(true, 2),
    ZX(true, 2),
    X(true, 2),
    Z(true, 2),
    QUAD(true, 4),
    DIAG(true, 4),
    NONE(true, 1);

    private final int numSymPoints;
    private final boolean perfectSymmetry;

    Symmetry(boolean perfectSymmetry, int numSymPoints) {
        this.perfectSymmetry = perfectSymmetry;
        this.numSymPoints = numSymPoints;
    }
}