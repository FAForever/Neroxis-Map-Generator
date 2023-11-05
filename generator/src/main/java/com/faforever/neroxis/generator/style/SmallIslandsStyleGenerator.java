package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.terrain.SmallIslandsTerrainGenerator;

import java.util.Arrays;

public class SmallIslandsStyleGenerator extends StyleGenerator {
    public SmallIslandsStyleGenerator() {
        weight = 4;
    }

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .landDensity(0f, .5f)
                                   .plateauDensity(0, .5f)
                                   .mexDensity(.25f, .75f)
                                   .mapSizes(768, 1024)
                                   .build();
    }

    @Override
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);
        terrainGenerator = new SmallIslandsTerrainGenerator();
        propGenerators.addAll(
                Arrays.asList(new BasicPropGenerator(), new NavyWrecksPropGenerator(), new RockFieldPropGenerator(),
                              new SmallBattlePropGenerator()));
    }
}


