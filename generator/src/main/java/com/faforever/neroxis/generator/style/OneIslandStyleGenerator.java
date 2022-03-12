package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.terrain.OneIslandTerrainGenerator;

import java.util.Arrays;

public strictfp class OneIslandStyleGenerator extends StyleGenerator {

    public OneIslandStyleGenerator() {
        name = "ONE_ISLAND";
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .75f)
                .plateauDensity(0f, .75f)
                .mapSizes(384, 1024)
                .build();
    }

    @Override
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);
        terrainGenerator = new OneIslandTerrainGenerator();
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new NavyWrecksPropGenerator(),
                new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}


