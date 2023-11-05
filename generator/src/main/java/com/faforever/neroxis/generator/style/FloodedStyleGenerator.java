package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorOptions;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.resource.WaterMexResourceGenerator;
import com.faforever.neroxis.generator.terrain.FloodedTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

import java.util.List;

public class FloodedStyleGenerator extends StyleGenerator {
    public FloodedStyleGenerator() {
        weight = 0f;
    }

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .plateauDensity(0, .25f)
                                   .landDensity(0, .5f)
                                   .mapSizes(384, 1024)
                                   .build();
    }

    @Override
    protected GeneratorOptions<TerrainGenerator> getTerrainGeneratorOptions() {
        return new GeneratorOptions<>(new FloodedTerrainGenerator());
    }

    @Override
    protected GeneratorOptions<ResourceGenerator> getResourceGeneratorOptions() {
        return new GeneratorOptions<>(new WaterMexResourceGenerator());
    }

    @Override
    protected GeneratorOptions<PropGenerator> getPropGeneratorOptions() {
        return new GeneratorOptions<>(new BasicPropGenerator(),
                                      List.of(new BasicPropGenerator(), new NavyWrecksPropGenerator()));
    }
}


