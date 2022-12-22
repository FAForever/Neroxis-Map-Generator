package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.*;
import com.faforever.neroxis.generator.terrain.CenterLakeTerrainGenerator;

import java.util.Arrays;

public class CenterLakeStyleGenerator extends StyleGenerator {
    public CenterLakeStyleGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                                                   .landDensity(0f, .5f)
                                                   .rampDensity(.75f, 1f)
                                                   .mexDensity(.25f, 1)
                                                   .mapSizes(384, 1024)
                                                   .build();
    }

    @Override
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);
        terrainGenerator = new CenterLakeTerrainGenerator();
        propGenerators.addAll(
                Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new NavyWrecksPropGenerator(),
                              new NeutralCivPropGenerator(), new RockFieldPropGenerator(),
                              new SmallBattlePropGenerator()));
    }
}


