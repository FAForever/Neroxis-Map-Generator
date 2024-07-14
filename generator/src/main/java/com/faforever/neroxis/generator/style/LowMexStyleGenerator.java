package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.BoulderFieldPropGenerator;
import com.faforever.neroxis.generator.prop.EnemyCivPropGenerator;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.resource.LowMexResourceGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.generator.terrain.OneIslandTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;

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
                                              new WeightedOption<>(new BoulderFieldPropGenerator(), .1f),
                                              new WeightedOption<>(new EnemyCivPropGenerator(), .5f),
                                              new WeightedOption<>(new HighReclaimPropGenerator(), .25f),
                                              new WeightedOption<>(new LargeBattlePropGenerator(), .5f),
                                              new WeightedOption<>(new NavyWrecksPropGenerator(), 2f),
                                              new WeightedOption<>(new NeutralCivPropGenerator(), 1f),
                                              new WeightedOption<>(new RockFieldPropGenerator(), 1f),
                                              new WeightedOption<>(new SmallBattlePropGenerator(), 1f));
    }
}


