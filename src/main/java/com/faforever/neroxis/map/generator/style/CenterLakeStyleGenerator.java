package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.prop.*;
import com.faforever.neroxis.map.generator.terrain.CenterLakeTerrainGenerator;

import java.util.Arrays;

public strictfp class CenterLakeStyleGenerator extends StyleGenerator {

    public CenterLakeStyleGenerator() {
        name = "CENTER_LAKE";
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .5f)
                .rampDensity(.75f, 1f)
                .mexDensity(.25f, 1)
                .mapSizes(512, 1024)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerator = new CenterLakeTerrainGenerator();
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(),
                new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }

}


