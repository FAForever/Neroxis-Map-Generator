package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

public class CustomStyleGenerator extends StyleGenerator {
    private ParameterConstraints parameterConstraints;

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                getGeneratorParameters().terrainGenerator().getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                getGeneratorParameters().resourceGenerator().getGeneratorSupplier().get());
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(
                getGeneratorParameters().propGenerator().getGeneratorSupplier().get());
    }

    @Override
    public ParameterConstraints getParameterConstraints() {
        return parameterConstraints;
    }

    public void setParameterConstraints(GeneratorParameters generatorParameters) {
        TerrainGenerator terrainGenerator = generatorParameters.terrainGenerator().getGeneratorSupplier().get();
        ResourceGenerator resourceGenerator = generatorParameters.resourceGenerator().getGeneratorSupplier().get();
        PropGenerator propGenerator = generatorParameters.propGenerator().getGeneratorSupplier().get();
        parameterConstraints = ParameterConstraints.builder()
                .landDensityRange(terrainGenerator.getParameterConstraints().landDensityRange())
                .mountainDensityRange(terrainGenerator.getParameterConstraints().mountainDensityRange())
                .plateauDensityRange(terrainGenerator.getParameterConstraints().plateauDensityRange())
                .rampDensityRange(terrainGenerator.getParameterConstraints().rampDensityRange())
                .mexDensityRange(resourceGenerator.getParameterConstraints().mexDensityRange())
                .reclaimDensityRange(propGenerator.getParameterConstraints().reclaimDensityRange())
                .build();
    }
}
