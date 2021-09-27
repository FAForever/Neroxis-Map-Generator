package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.map.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.map.generator.resource.WaterMexResourceGenerator;
import com.faforever.neroxis.map.generator.terrain.FloodedTerrainGenerator;

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


