package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedConstrainedOptions;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.terrain.OneIslandTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

public class OneIslandStyleGenerator extends StyleGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .landDensity(0f, .75f)
                                   .plateauDensity(0f, .75f)
                                   .mapSizes(384, 1024)
                                   .build();
    }

    @Override
    protected WeightedConstrainedOptions<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedConstrainedOptions.single(new OneIslandTerrainGenerator());
    }

    @Override
    protected WeightedConstrainedOptions<PropGenerator> getPropGeneratorOptions() {
        return new WeightedConstrainedOptions<>(new BasicPropGenerator(),
                                                new WeightedOption<>(new BasicPropGenerator(), 1f),
                                                new WeightedOption<>(new NavyWrecksPropGenerator(), 2f),
                                                new WeightedOption<>(new NeutralCivPropGenerator(), 1f),
                                                new WeightedOption<>(new RockFieldPropGenerator(), 1f),
                                                new WeightedOption<>(new SmallBattlePropGenerator(), 1f));
    }
}


