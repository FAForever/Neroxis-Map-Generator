package com.faforever.neroxis.util;

public strictfp class EndianSwapper {

    public static short swap(short value) {
        int b1 = value & 0xff;
        int b2 = (value >> 8) & 0xff;

        return (short) (b1 << 8 | b2);
    }

    public static int swap(int value) {
        int b1 = (value) & 0xff;
        int b2 = (value >> 8) & 0xff;
        int b3 = (value >> 16) & 0xff;
        int b4 = (value >> 24) & 0xff;

        return b1 << 24 | b2 << 16 | b3 << 8 | b4;
    }

    public static float swap(float value) {
        int intValue = Float.floatToRawIntBits(value);
        intValue = swap(intValue);

        return Float.intBitsToFloat(intValue);
    }
}