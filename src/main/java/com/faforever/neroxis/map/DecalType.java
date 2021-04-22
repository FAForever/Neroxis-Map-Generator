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
        switch (typeNum) {
            case 1:
                return ALBEDO;
            case 2:
                return NORMALS;
            case 3:
                return WATER_MASK;
            case 4:
                return WATER_ALBEDO;
            case 5:
                return WATER_NORMALS;
            case 6:
                return GLOW;
            case 7:
                return ALPHA_NORMALS;
            case 8:
                return GLOW_MASK;
            default:
                return null;
        }
    }

    public int getTypeNum() {
        return typeNum;
    }
}