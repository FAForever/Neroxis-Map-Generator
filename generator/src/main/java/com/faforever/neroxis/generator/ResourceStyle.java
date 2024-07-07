package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.resource.BasicResourceGenerator;
import com.faforever.neroxis.generator.resource.LowMexResourceGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.resource.WaterMexResourceGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum ResourceStyle {
    BASIC(BasicResourceGenerator::new),
    LOW_MEX(LowMexResourceGenerator::new),
    WATER_MEX(WaterMexResourceGenerator::new);

    private final Supplier<ResourceGenerator> generatorSupplier;
}
