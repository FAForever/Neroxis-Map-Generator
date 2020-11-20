package map;

import lombok.Getter;

@Getter
public strictfp enum Symmetry {
    POINT2(2),
    POINT4(4),
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