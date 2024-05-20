package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.*;
import com.faforever.neroxis.generator.resource.LowMexResourceGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.*;

public class LowMexStyleGenerator extends StyleGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mapSizes(256, 640)
                                   .spawnCount(0, 4)
                                   .numTeams(2, 2)
                                   .build();
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new LowMexResourceGenerator());
    }

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicTerrainGenerator(),
                                              new WeightedOption<>(new BasicTerrainGenerator(), 1f),
                                              new WeightedOption<>(new OneIslandTerrainGenerator(), 1f),
                                              new WeightedOption<>(new LittleMountainTerrainGenerator(), 1f),
                                              new WeightedOption<>(new ValleyTerrainGenerator(), 1f));
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicPropGenerator(),
                                              new WeightedOption<>(new BasicPropGenerator(), 1f),
                                              new WeightedOption<>(new NavyWrecksPropGenerator(), 2f),
                                              new WeightedOption<>(new EnemyCivPropGenerator(), .5f),
                                              new WeightedOption<>(new LargeBattlePropGenerator(), 2f),
                                              new WeightedOption<>(new NeutralCivPropGenerator(), 1f),
                                              new WeightedOption<>(new RockFieldPropGenerator(), 1f),
                                              new WeightedOption<>(new SmallBattlePropGenerator(), 1f),
                                              new WeightedOption<>(new HighReclaimPropGenerator(), .5f));
    }
}


