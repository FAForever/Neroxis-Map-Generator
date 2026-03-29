package com.faforever.neroxis.map;

import lombok.RequiredArgsConstructor;


public sealed interface DecalType {

    static DecalType of(int typeNum) {
        return switch (typeNum) {
            case 1 -> Known.ALBEDO;
            case 2 -> Known.NORMALS;
            case 3 -> Known.WATER_MASK;
            case 4 -> Known.WATER_ALBEDO;
            case 5 -> Known.WATER_NORMALS;
            case 6 -> Known.GLOW;
            case 7 -> Known.ALPHA_NORMALS;
            case 8 -> Known.GLOW_MASK;
            default -> new Unknown(typeNum);
        };
    }

    int typeNum();

    @RequiredArgsConstructor
    enum Known implements DecalType {
        ALBEDO(1), NORMALS(2), WATER_MASK(3), WATER_ALBEDO(4), WATER_NORMALS(5), GLOW(6), ALPHA_NORMALS(7), GLOW_MASK(
                8);
        private final int typeNum;

        @Override
        public int typeNum() {
            return typeNum;
        }
    }

    record Unknown(int typeNum) implements DecalType {}
}
