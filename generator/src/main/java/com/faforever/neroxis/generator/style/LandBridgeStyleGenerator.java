package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.*;
import com.faforever.neroxis.generator.terrain.LandBridgeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

public class LandBridgeStyleGenerator extends StyleGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mapSizes(768, 1024)
                                   .numTeams(2, 4)
                                   .build();
    }

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new LandBridgeTerrainGenerator());
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new LargeBattlePropGenerator(),
                                              new WeightedOption<>(new LargeBattlePropGenerator(), 2f),
                                              new WeightedOption<>(new NeutralCivPropGenerator(), 1f),
                                              new WeightedOption<>(new RockFieldPropGenerator(), 1f),
                                              new WeightedOption<>(new SmallBattlePropGenerator(), 1f),
                                              new WeightedOption<>(new NavyWrecksPropGenerator(), 2f));
    }

    @Override
    public float getSpawnSeparation() {
        return getGeneratorParameters().mapSize() / 8f;
    }
}

