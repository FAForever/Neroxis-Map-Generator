package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.map.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.map.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.map.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.map.generator.terrain.SmallIslandsTerrainGenerator;

import java.util.Arrays;

public strictfp class SmallIslandsStyleGenerator extends StyleGenerator {

    public SmallIslandsStyleGenerator() {
        name = "SMALL_ISLANDS";
        weight = 2;
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .5f)
                .plateauDensity(0, .5f)
                .mexDensity(.25f, .75f)
                .mapSizes(1024)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        teamSeparation = mapParameters.getMapSize() / 2;
        terrainGenerator = new SmallIslandsTerrainGenerator();
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new NavyWrecksPropGenerator(),
                new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}


