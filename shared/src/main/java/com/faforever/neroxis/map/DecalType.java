package com.faforever.neroxis.map;

import lombok.Getter;

@Getter
public strictfp enum DecalType {
    ALBEDO(1),
    NORMALS(2),
    WATER_MASK(3),
    WATER_ALBEDO(4),
    WATER_NORMALS(5),
    GLOW(6),
    ALPHA_NORMALS(7),
    GLOW_MASK(8);

    private final int typeNum;

    DecalType(int typeNum) {
        this.typeNum = typeNum;
    }

    public static DecalType of(int typeNum) {
        return switch (typeNum) {
            case 1 -> ALBEDO;
            case 2 -> NORMALS;
            case 3 -> WATER_MASK;
            case 4 -> WATER_ALBEDO;
            case 5 -> WATER_NORMALS;
            case 6 -> GLOW;
            case 7 -> ALPHA_NORMALS;
            case 8 -> GLOW_MASK;
            default -> null;
        };
    }

    public int getTypeNum() {
        return typeNum;
    }
}