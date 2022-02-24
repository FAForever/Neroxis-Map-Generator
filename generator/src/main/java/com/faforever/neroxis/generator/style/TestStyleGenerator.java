package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.map.MapParameters;
import lombok.Getter;

@Getter
public strictfp class TestStyleGenerator extends StyleGenerator {

    public TestStyleGenerator() {
        name = "TEST";
        weight = 0;
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
    }
}

