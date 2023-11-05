package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.EnemyCivPropGenerator;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.resource.LowMexResourceGenerator;
import com.faforever.neroxis.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.generator.terrain.OneIslandTerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;

import java.util.Arrays;

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
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);
        resourceGenerator = new LowMexResourceGenerator();
        terrainGenerators.addAll(Arrays.asList(new BasicTerrainGenerator(), new OneIslandTerrainGenerator(),
                                               new LittleMountainTerrainGenerator(), new ValleyTerrainGenerator()));
        propGenerators.addAll(
                Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                              new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(),
                              new RockFieldPropGenerator(), new SmallBattlePropGenerator(),
                              new HighReclaimPropGenerator()));
    }
}


