package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.biomes.Biomes;

public class SyrtisTextureGenerator extends LegacyTextureGenerator {

    @Override
    public void loadBiome() {
        biome = Biomes.loadBiome(BiomeName.SYRTIS);
    }
}
