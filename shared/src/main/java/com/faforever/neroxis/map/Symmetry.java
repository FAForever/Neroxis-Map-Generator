package com.faforever.neroxis.map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Symmetry {
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

    private final boolean perfectSymmetry;
    private final int numSymPoints;
    public final boolean isOddSymmetry() { return !perfectSymmetry; }
}