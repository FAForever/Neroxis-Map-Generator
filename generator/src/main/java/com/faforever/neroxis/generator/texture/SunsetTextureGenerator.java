package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.biomes.Biomes;

public class SunsetTextureGenerator extends PbrTextureGenerator{

    @Override
    public Biome loadBiome() {
        return Biomes.loadBiome(BiomeName.SUNSET);
    }
}
