package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedConstrainedOptions;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.terrain.LandBridgeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

public class LandBridgeStyleGenerator extends StyleGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .landDensity(.25f, .75f)
                                   .mexDensity(.5f, 1f)
                                   .reclaimDensity(.5f, 1f)
                                   .mapSizes(768, 1024)
                                   .numTeams(2, 4)
                                   .build();
    }

    @Override
    protected WeightedConstrainedOptions<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedConstrainedOptions.single(new LandBridgeTerrainGenerator());
    }

    @Override
    protected WeightedConstrainedOptions<PropGenerator> getPropGeneratorOptions() {
        return new WeightedConstrainedOptions<>(new LargeBattlePropGenerator(),
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

