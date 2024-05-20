package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.terrain.*;
import com.faforever.neroxis.generator.texture.*;

public class HighReclaimStyleGenerator extends StyleGenerator {

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicTerrainGenerator(),
                                              new WeightedOption<>(new DropPlateauTerrainGenerator(), 1f),
                                              new WeightedOption<>(new MountainRangeTerrainGenerator(), 1f),
                                              new WeightedOption<>(new LittleMountainTerrainGenerator(), 1f),
                                              new WeightedOption<>(new ValleyTerrainGenerator(), 1f));
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new HighReclaimPropGenerator());
    }

    @Override
    protected WeightedOptionsWithFallback<TextureGenerator> getTextureGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new DesertTextureGenerator(),
                                              new WeightedOption<>(new DesertTextureGenerator(), 1f),
                                              new WeightedOption<>(new FrithenTextureGenerator(), 1f),
                                              new WeightedOption<>(new MoonlightTextureGenerator(), 1f),
                                              new WeightedOption<>(new SunsetTextureGenerator(), 1f),
                                              new WeightedOption<>(new WonderTextureGenerator(), 1f));
    }
}


