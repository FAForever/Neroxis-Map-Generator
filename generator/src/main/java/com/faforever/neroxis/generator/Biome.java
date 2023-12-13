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

    public static Biome getByValue(String value) {
        for (Biome biome : values()) {
            if (biome.getValue().equalsIgnoreCase(value)) {
                return biome;
            }
        }
        return null;
    }
}
