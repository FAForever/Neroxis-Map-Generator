package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.*;
import com.faforever.neroxis.generator.terrain.MountainRangeTerrainGenerator;

import java.util.Arrays;

public class MountainRangeStyleGenerator extends StyleGenerator {
    public MountainRangeStyleGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                                                   .landDensity(.75f, 1f)
                                                   .mountainDensity(.5f, 1)
                                                   .plateauDensity(0, .5f)
                                                   .mexDensity(.375f, 1)
                                                   .mapSizes(256, 640)
                                                   .build();
    }

    @Override
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);
        terrainGenerator = new MountainRangeTerrainGenerator();
        propGenerators.addAll(
                Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                              new NeutralCivPropGenerator(), new RockFieldPropGenerator(),
                              new SmallBattlePropGenerator(), new HighReclaimPropGenerator()));
    }
}
