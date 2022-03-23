package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import lombok.Getter;

@Getter
public strictfp class TestStyleGenerator extends StyleGenerator {

    public TestStyleGenerator() {
        weight = 0;
    }

    @Override
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);
    }
}

