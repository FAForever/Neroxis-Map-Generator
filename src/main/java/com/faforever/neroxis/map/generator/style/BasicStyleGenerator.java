package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.generator.prop.*;
import lombok.Getter;

import java.util.Arrays;

@Getter
public strictfp class BasicStyleGenerator extends StyleGenerator {

    public BasicStyleGenerator() {
        name = "BASIC";
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}

