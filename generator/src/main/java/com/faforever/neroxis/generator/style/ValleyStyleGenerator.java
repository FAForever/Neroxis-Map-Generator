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
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;
import java.util.Arrays;

public strictfp class ValleyStyleGenerator extends StyleGenerator {
    public ValleyStyleGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                                                   .landDensity(.75f, 1f)
                                                   .mountainDensity(.5f, 1)
                                                   .mapSizes(384, 1024)
                                                   .build();
    }

    @Override
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);
        terrainGenerator = new ValleyTerrainGenerator();
        propGenerators.addAll(
                Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                              new NeutralCivPropGenerator(), new RockFieldPropGenerator(),
                              new SmallBattlePropGenerator(), new HighReclaimPropGenerator()));
    }
}

