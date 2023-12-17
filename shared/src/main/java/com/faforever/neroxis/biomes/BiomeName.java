package com.faforever.neroxis.biomes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BiomeName {
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

    public static BiomeName getByValue(String value) {
        for (BiomeName biomeName : values()) {
            if (biomeName.getValue().equalsIgnoreCase(value)) {
                return biomeName;
            }
        }
        return null;
    }
}
