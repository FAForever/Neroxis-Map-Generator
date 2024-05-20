package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.terrain.*;

import static com.faforever.neroxis.biomes.BiomeName.*;

public class HighReclaimStyleGenerator extends StyleGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .biomes(DESERT, FRITHEN, MOONLIGHT, SUNSET, WONDER)
                                   .build();
    }

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
}


