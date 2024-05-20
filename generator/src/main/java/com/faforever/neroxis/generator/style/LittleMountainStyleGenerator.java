package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.*;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

public class LittleMountainStyleGenerator extends StyleGenerator {

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new LittleMountainTerrainGenerator());
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicPropGenerator(),
                                              new WeightedOption<>(new BasicPropGenerator(), 1f),
                                              new WeightedOption<>(new EnemyCivPropGenerator(), .5f),
                                              new WeightedOption<>(new LargeBattlePropGenerator(), 2f),
                                              new WeightedOption<>(new NeutralCivPropGenerator(), 1f),
                                              new WeightedOption<>(new RockFieldPropGenerator(), 1f),
                                              new WeightedOption<>(new SmallBattlePropGenerator(), 1f),
                                              new WeightedOption<>(new HighReclaimPropGenerator(), .5f));
    }
}
