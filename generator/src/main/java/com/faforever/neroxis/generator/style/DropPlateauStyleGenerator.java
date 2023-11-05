package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorOptions;
import com.faforever.neroxis.generator.ParameterConstraints;
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

import java.util.List;

public class DropPlateauStyleGenerator extends StyleGenerator {
    public DropPlateauStyleGenerator() {
        weight = .5f;
    }

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .landDensity(.5f, 1f)
                                   .plateauDensity(.5f, 1)
                                   .mexDensity(.25f, 1)
                                   .build();
    }

    @Override
    protected GeneratorOptions<TerrainGenerator> getTerrainGeneratorOptions() {
        return new GeneratorOptions<>(new DropPlateauTerrainGenerator());
    }

    @Override
    protected GeneratorOptions<PropGenerator> getPropGeneratorOptions() {
        return new GeneratorOptions<>(new BasicPropGenerator(),
                                      List.of(new BasicPropGenerator(), new EnemyCivPropGenerator(),
                                              new LargeBattlePropGenerator(),
                                              new NeutralCivPropGenerator(), new RockFieldPropGenerator(),
                                              new SmallBattlePropGenerator(), new HighReclaimPropGenerator()));
    }
}


