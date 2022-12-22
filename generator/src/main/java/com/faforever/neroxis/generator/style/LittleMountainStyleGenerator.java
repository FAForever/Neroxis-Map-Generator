package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.*;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;

import java.util.Arrays;

public class LittleMountainStyleGenerator extends StyleGenerator {
    public LittleMountainStyleGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                                                   .landDensity(.5f, 1f)
                                                   .mountainDensity(.25f, 1)
                                                   .plateauDensity(0, .5f)
                                                   .build();
    }

    @Override
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);
        terrainGenerator = new LittleMountainTerrainGenerator();
        propGenerators.addAll(
                Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                              new NeutralCivPropGenerator(), new RockFieldPropGenerator(),
                              new SmallBattlePropGenerator(), new HighReclaimPropGenerator()));
    }
}
