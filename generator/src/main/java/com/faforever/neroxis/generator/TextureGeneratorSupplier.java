package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.texture.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum TextureGeneratorSupplier {
    BRIMSTONE(BrimstoneTextureGenerator::new, "Brimstone"),
    DESERT(DesertTextureGenerator::new, "Desert"),
    EARLYAUTUMN(EarlyAutumnTextureGenerator::new, "EarlyAutumn"),
    FRITHEN(FrithenTextureGenerator::new, "Frithen"),
    MARS(MarsTextureGenerator::new, "Mars"),
    MOONLIGHT(MoonlightTextureGenerator::new, "Moonlight"),
    PRAYER(PrayerTextureGenerator::new, "Prayer"),
    STONES(StonesTextureGenerator::new, "Stones"),
    SUNSET(SunsetTextureGenerator::new, "Sunset"),
    SYRTIS(SyrtisTextureGenerator::new, "Syrtis"),
    WINDINGRIVER(WindingRiverTextureGenerator::new, "WindingRiver"),
    WONDER(WonderTextureGenerator::new, "Wonder");

    private final Supplier<com.faforever.neroxis.generator.texture.TextureGenerator> generatorSupplier;
    private final String folderName;
}
