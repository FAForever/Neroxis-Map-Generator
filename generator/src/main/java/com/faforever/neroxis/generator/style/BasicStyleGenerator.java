package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.prop.*;
import lombok.Getter;

import java.util.Arrays;

@Getter
public class BasicStyleGenerator extends StyleGenerator {
    @Override
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);
        propGenerators.addAll(
                Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                              new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(),
                              new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}

