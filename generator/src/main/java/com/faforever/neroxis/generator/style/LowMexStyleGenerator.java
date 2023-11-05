package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorOptions;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
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

import java.util.List;

public class LowMexStyleGenerator extends StyleGenerator {
    public LowMexStyleGenerator() {
        weight = .5f;
    }

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mexDensity(0f, .25f)
                                   .mapSizes(256, 640)
                                   .spawnCount(0, 4)
                                   .numTeams(2, 2)
                                   .build();
    }

    @Override
    protected GeneratorOptions<ResourceGenerator> getResourceGeneratorOptions() {
        return new GeneratorOptions<>(new LowMexResourceGenerator());
    }

    @Override
    protected GeneratorOptions<TerrainGenerator> getTerrainGeneratorOptions() {
        return new GeneratorOptions<>(new BasicTerrainGenerator(),
                                      List.of(new BasicTerrainGenerator(), new OneIslandTerrainGenerator(),
                                              new LittleMountainTerrainGenerator(), new ValleyTerrainGenerator()));
    }

    @Override
    protected GeneratorOptions<PropGenerator> getPropGeneratorOptions() {
        return new GeneratorOptions<>(new BasicPropGenerator(),
                                      List.of(new BasicPropGenerator(), new EnemyCivPropGenerator(),
                                              new LargeBattlePropGenerator(),
                                              new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(),
                                              new RockFieldPropGenerator(), new SmallBattlePropGenerator(),
                                              new HighReclaimPropGenerator()));
    }
}


