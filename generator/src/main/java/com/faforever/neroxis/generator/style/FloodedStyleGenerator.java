package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.resource.WaterMexResourceGenerator;
import com.faforever.neroxis.generator.terrain.FloodedTerrainGenerator;
import com.faforever.neroxis.map.MapParameters;

import java.util.Arrays;

public strictfp class FloodedStyleGenerator extends StyleGenerator {

    public FloodedStyleGenerator() {
        name = "FLOODED";
        parameterConstraints = ParameterConstraints.builder()
                .plateauDensity(0, .25f)
                .landDensity(0, .5f)
                .mapSizes(384, 1024)
                .build();
        weight = 0f;
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerator = new FloodedTerrainGenerator();
        resourceGenerator = new WaterMexResourceGenerator();
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(),
                new NavyWrecksPropGenerator()));
    }

}

