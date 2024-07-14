package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.texture.BrimstoneTextureGenerator;
import com.faforever.neroxis.generator.texture.DesertTextureGenerator;
import com.faforever.neroxis.generator.texture.EarlyAutumnTextureGenerator;
import com.faforever.neroxis.generator.texture.FrithenTextureGenerator;
import com.faforever.neroxis.generator.texture.MarsTextureGenerator;
import com.faforever.neroxis.generator.texture.MoonlightTextureGenerator;
import com.faforever.neroxis.generator.texture.PrayerTextureGenerator;
import com.faforever.neroxis.generator.texture.StonesTextureGenerator;
import com.faforever.neroxis.generator.texture.SunsetTextureGenerator;
import com.faforever.neroxis.generator.texture.SyrtisTextureGenerator;
import com.faforever.neroxis.generator.texture.TextureGenerator;
import com.faforever.neroxis.generator.texture.WindingRiverTextureGenerator;
import com.faforever.neroxis.generator.texture.WonderTextureGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum TextureStyle {
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

    private final Supplier<TextureGenerator> generatorSupplier;
    private final String folderName;
}
