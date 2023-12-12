package com.faforever.neroxis.generator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Biome {
    BRIMSTONE("Brimstone"),
    DESERT("Desert"),
    EARLYAUTUMN("EarlyAutumn"),
    FRITHEN("Frithen"),
    LOKI("Loki"),
    MARS("Mars"),
    MOONLIGHT("Moonlight"),
    PRAYER("Prayer"),
    STONES("Stones"),
    SYRTIS("Syrtis"),
    WINDINGRIVER("WindingRiver"),
    WONDER("Wonder");

    private final String value;

    public static int getIndexByValue(String value) {
        for (int i = 0; i < values().length; i++) {
            if (values()[i].getValue().equals(value)) {
                return i;
            }
        }
        return -1;
    }
}
