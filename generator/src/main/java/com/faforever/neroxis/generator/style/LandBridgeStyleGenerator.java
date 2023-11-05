package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorOptions;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.terrain.LandBridgeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

import java.util.List;

public class LandBridgeStyleGenerator extends StyleGenerator {
    public LandBridgeStyleGenerator() {
        weight = 2;
    }

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
    protected GeneratorOptions<TerrainGenerator> getTerrainGeneratorOptions() {
        return new GeneratorOptions<>(new LandBridgeTerrainGenerator());
    }

    @Override
    protected GeneratorOptions<PropGenerator> getPropGeneratorOptions() {
        return new GeneratorOptions<>(new BasicPropGenerator(),
                                      List.of(new LargeBattlePropGenerator(), new NavyWrecksPropGenerator(),
                                              new NeutralCivPropGenerator(), new RockFieldPropGenerator(),
                                              new SmallBattlePropGenerator()));
    }

    @Override
    public float getSpawnSeparation() {
        return getGeneratorParameters().mapSize() / 8f;
    }
}

