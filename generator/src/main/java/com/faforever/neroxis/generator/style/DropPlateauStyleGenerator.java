package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedConstrainedOptions;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.EnemyCivPropGenerator;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.terrain.DropPlateauTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

public class DropPlateauStyleGenerator extends StyleGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .landDensity(.5f, 1f)
                                   .plateauDensity(.5f, 1)
                                   .mexDensity(.25f, 1)
                                   .build();
    }

    @Override
    protected WeightedConstrainedOptions<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedConstrainedOptions.single(new DropPlateauTerrainGenerator());
    }

    @Override
    protected WeightedConstrainedOptions<PropGenerator> getPropGeneratorOptions() {
        return new WeightedConstrainedOptions<>(new BasicPropGenerator(),
                                                new WeightedOption<>(new BasicPropGenerator(), 1f),
                                                new WeightedOption<>(new EnemyCivPropGenerator(), .5f),
                                                new WeightedOption<>(new LargeBattlePropGenerator(), 2f),
                                                new WeightedOption<>(new NeutralCivPropGenerator(), 1f),
                                                new WeightedOption<>(new RockFieldPropGenerator(), 1f),
                                                new WeightedOption<>(new SmallBattlePropGenerator(), 1f),
                                                new WeightedOption<>(new HighReclaimPropGenerator(), .5f));
    }
}


