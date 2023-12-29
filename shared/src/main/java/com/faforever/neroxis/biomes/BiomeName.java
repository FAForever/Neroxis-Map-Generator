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

    private final String folderName;
}
