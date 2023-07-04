package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.EnemyCivPropGenerator;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.terrain.DropPlateauSpawnFirstTerrainGenerator;

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
        terrainGenerator = new DropPlateauSpawnFirstTerrainGenerator();
        propGenerators.addAll(
                Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                              new NeutralCivPropGenerator(), new RockFieldPropGenerator(),
                              new SmallBattlePropGenerator(), new HighReclaimPropGenerator()));
    }
}


