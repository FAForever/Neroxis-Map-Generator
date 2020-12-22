package map;

import lombok.Getter;

@Getter
public strictfp enum Symmetry {
    POINT2(2),
    POINT3(3),
    POINT4(4),
    POINT5(5),
    POINT6(6),
    POINT7(7),
    POINT8(8),
    POINT9(9),
    POINT10(10),
    POINT11(11),
    POINT12(12),
    POINT13(13),
    POINT14(14),
    POINT15(15),
    POINT16(16),
    XZ(2),
    ZX(2),
    X(2),
    Z(2),
    QUAD(4),
    DIAG(4),
    NONE(1);

    private final int numSymPoints;

    Symmetry(int numSymPoints) {
        this.numSymPoints = numSymPoints;
    }
}