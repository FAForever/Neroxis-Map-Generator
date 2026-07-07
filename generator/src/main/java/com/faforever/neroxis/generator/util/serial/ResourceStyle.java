package com.faforever.neroxis.generator.util.serial;

import com.faforever.neroxis.generator.resource.BasicResourceGenerator;
import com.faforever.neroxis.generator.resource.HighMexLandLowMexWaterResourceGenerator;
import com.faforever.neroxis.generator.resource.LowMexResourceGenerator;
import com.faforever.neroxis.generator.resource.OneHydroFourMexResourceGenerator;
import com.faforever.neroxis.generator.resource.OneHydroPerSpawnResourceGenerator;
import com.faforever.neroxis.generator.resource.WaterMexResourceGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum ResourceStyle {
    BASIC(BasicResourceGenerator::new),
    LOW_MEX(LowMexResourceGenerator::new),
    WATER_MEX(WaterMexResourceGenerator::new),
    HI_MEX_LAND_LOW_MEX_WATER(HighMexLandLowMexWaterResourceGenerator::new),
    ONE_HYDRO_NO_MEX(OneHydroPerSpawnResourceGenerator::new),
    ONE_HYDRO_FOUR_MEX(OneHydroFourMexResourceGenerator::new);

    private final Supplier<com.faforever.neroxis.generator.resource.ResourceGenerator> generatorSupplier;
}
