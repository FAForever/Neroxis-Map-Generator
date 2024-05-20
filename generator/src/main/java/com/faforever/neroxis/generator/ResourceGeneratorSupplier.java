package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.resource.BasicResourceGenerator;
import com.faforever.neroxis.generator.resource.LowMexResourceGenerator;
import com.faforever.neroxis.generator.resource.WaterMexResourceGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum ResourceGeneratorSupplier {
    BASIC(BasicResourceGenerator.class, BasicResourceGenerator::new),
    LOW_MEX(LowMexResourceGenerator.class, LowMexResourceGenerator::new),
    WATER_MEX(WaterMexResourceGenerator.class, WaterMexResourceGenerator::new);

    private final Class<? extends com.faforever.neroxis.generator.resource.ResourceGenerator> generatorClass;
    private final Supplier<com.faforever.neroxis.generator.resource.ResourceGenerator> generatorSupplier;
}
