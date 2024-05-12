package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.resource.BasicResourceGenerator;
import com.faforever.neroxis.generator.resource.LowMexResourceGenerator;
import com.faforever.neroxis.generator.resource.WaterMexResourceGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum ResourceGenerator {
    BASIC(BasicResourceGenerator.class, BasicResourceGenerator::new, 1),
    LOW_MEX(LowMexResourceGenerator.class, LowMexResourceGenerator::new, 1),
    WATER_MEX(WaterMexResourceGenerator.class, WaterMexResourceGenerator::new, 1);

    private final Class<? extends com.faforever.neroxis.generator.resource.ResourceGenerator> generatorClass;
    private final Supplier<com.faforever.neroxis.generator.resource.ResourceGenerator> generatorSupplier;
    private final float weight;
}
