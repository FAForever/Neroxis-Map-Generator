package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.texture.BrimstoneTextureGenerator;
import com.faforever.neroxis.generator.texture.CrystallineTextureGenerator;
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
    BRIMSTONE(BrimstoneTextureGenerator::new),
    DESERT(DesertTextureGenerator::new),
    EARLYAUTUMN(EarlyAutumnTextureGenerator::new),
    FRITHEN(FrithenTextureGenerator::new),
    MARS(MarsTextureGenerator::new),
    MOONLIGHT(MoonlightTextureGenerator::new),
    PRAYER(PrayerTextureGenerator::new),
    STONES(StonesTextureGenerator::new),
    SUNSET(SunsetTextureGenerator::new),
    SYRTIS(SyrtisTextureGenerator::new),
    WINDINGRIVER(WindingRiverTextureGenerator::new),
    WONDER(WonderTextureGenerator::new),
    CRYSTALLINE(CrystallineTextureGenerator::new);

    private final Supplier<TextureGenerator> generatorSupplier;
}
