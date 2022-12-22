package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.*;
import com.faforever.neroxis.generator.terrain.DropPlateauTerrainGenerator;

import java.util.Arrays;

public class DropPlateauStyleGenerator extends StyleGenerator {
    public DropPlateauStyleGenerator() {
        weight = .5f;
        parameterConstraints = ParameterConstraints.builder()
                                                   .landDensity(.5f, 1f)
                                                   .plateauDensity(.5f, 1)
                                                   .mexDensity(.25f, 1)
                                                   .build();
    }

    @Override
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);
        terrainGenerator = new DropPlateauTerrainGenerator();
        propGenerators.addAll(
                Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                              new NeutralCivPropGenerator(), new RockFieldPropGenerator(),
                              new SmallBattlePropGenerator(), new HighReclaimPropGenerator()));
    }
}


