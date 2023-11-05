package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorOptions;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.terrain.OneIslandTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

import java.util.List;

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
    protected GeneratorOptions<TerrainGenerator> getTerrainGeneratorOptions() {
        return new GeneratorOptions<>(new OneIslandTerrainGenerator());
    }

    @Override
    protected GeneratorOptions<PropGenerator> getPropGeneratorOptions() {
        return new GeneratorOptions<>(new BasicPropGenerator(),
                                      List.of(new BasicPropGenerator(), new NavyWrecksPropGenerator(),
                                              new NeutralCivPropGenerator(),
                                              new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}


