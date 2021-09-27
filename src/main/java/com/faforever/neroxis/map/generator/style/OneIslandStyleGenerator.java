package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.map.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.map.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.map.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.map.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.map.generator.terrain.OneIslandTerrainGenerator;

import java.util.Arrays;

public strictfp class OneIslandStyleGenerator extends StyleGenerator {

    public OneIslandStyleGenerator() {
        name = "ONE_ISLAND";
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .75f)
                .plateauDensity(0f, .75f)
                .mapSizes(384, 1024)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerator = new OneIslandTerrainGenerator();
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new NavyWrecksPropGenerator(),
                new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}


