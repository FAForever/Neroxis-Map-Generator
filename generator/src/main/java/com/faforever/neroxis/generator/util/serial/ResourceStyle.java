package com.faforever.neroxis.generator.util.serial;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.resource.BasicResourceGenerator;
import com.faforever.neroxis.generator.resource.HighMexLandLowMexWaterResourceGenerator;
import com.faforever.neroxis.generator.resource.LowMexResourceGenerator;
import com.faforever.neroxis.generator.resource.OneHydroFourMexResourceGenerator;
import com.faforever.neroxis.generator.resource.OneHydroPerSpawnResourceGenerator;
import com.faforever.neroxis.generator.resource.WaterMexResourceGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum ResourceStyle implements HasParameterConstraints {
    BASIC(BasicResourceGenerator::new, ParameterConstraints.ANY),
    LOW_MEX(LowMexResourceGenerator::new, ParameterConstraints.builder()
                                                              .mapSizes(384, 768)
                                                              .spawnCount(0, 4)
                                                              .build()),
    WATER_MEX(WaterMexResourceGenerator::new, ParameterConstraints.ANY),
    HI_MEX_LAND_LOW_MEX_WATER(HighMexLandLowMexWaterResourceGenerator::new, ParameterConstraints.ANY),
    ONE_HYDRO_NO_MEX(OneHydroPerSpawnResourceGenerator::new, ParameterConstraints.ANY),
    ONE_HYDRO_FOUR_MEX(OneHydroFourMexResourceGenerator::new, ParameterConstraints.ANY);

    private final Supplier<com.faforever.neroxis.generator.resource.ResourceGenerator> generatorSupplier;
    private final ParameterConstraints parameterConstraints;

    @JsonIgnore
    @Override
    public ParameterConstraints parameterConstraints() {
        return parameterConstraints;
    }
}
