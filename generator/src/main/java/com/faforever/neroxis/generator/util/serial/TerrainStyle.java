package com.faforever.neroxis.generator.util.serial;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.terrain.BasicLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.generator.terrain.BigIslandsTerrainGenerator;
import com.faforever.neroxis.generator.terrain.CenterLakeLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.CenterLakeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.DropPlateauLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.DropPlateauTerrainGenerator;
import com.faforever.neroxis.generator.terrain.FloodedTerrainGenerator;
import com.faforever.neroxis.generator.terrain.FractalLandLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.FractalNavyLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.FractalPlateauLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LandBridgeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MountainRangeLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MountainRangeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MultiLevelLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.OneIslandTerrainGenerator;
import com.faforever.neroxis.generator.terrain.RiversAndOceansTerrainGenerator;
import com.faforever.neroxis.generator.terrain.RiversTerrainGenerator;
import com.faforever.neroxis.generator.terrain.SetonishLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.SmallIslandsTerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum TerrainStyle implements HasParameterConstraints {
    BASIC(BasicTerrainGenerator::new, ParameterConstraints.ANY),
    BASIC_LAST(BasicLastTerrainGenerator::new, ParameterConstraints.ANY),
    BIG_ISLANDS(BigIslandsTerrainGenerator::new, ParameterConstraints.builder()
                                                                     .mapSizes(768, 1024)
                                                                     .build()),
    CENTER_LAKE(CenterLakeTerrainGenerator::new, ParameterConstraints.builder()
                                                                     .mapSizes(384, 1024)
                                                                     .build()),
    CENTER_LAKE_LAST(CenterLakeLastTerrainGenerator::new, ParameterConstraints.builder()
                                                                              .mapSizes(384, 1024)
                                                                              .build()),
    DROP_PLATEAU(DropPlateauTerrainGenerator::new, ParameterConstraints.ANY),
    DROP_PLATEAU_LAST(DropPlateauLastTerrainGenerator::new, ParameterConstraints.ANY),
    FLOODED(FloodedTerrainGenerator::new, ParameterConstraints.builder()
                                                              .mapSizes(384, 1024)
                                                              .build()),
    LAND_BRIDGE(LandBridgeTerrainGenerator::new, ParameterConstraints.builder()
                                                                     .mapSizes(768, 1024)
                                                                     .numTeams(2, 4)
                                                                     .build()),
    LITTLE_MOUNTAIN(LittleMountainTerrainGenerator::new, ParameterConstraints.ANY),
    LITTLE_MOUNTAIN_LAST(LittleMountainLastTerrainGenerator::new, ParameterConstraints.ANY),
    MOUNTAIN_RANGE(MountainRangeTerrainGenerator::new, ParameterConstraints.builder()
                                                                           .mapSizes(256, 768)
                                                                           .build()),
    MOUNTAIN_RANGE_LAST(MountainRangeLastTerrainGenerator::new, ParameterConstraints.builder()
                                                                                    .mapSizes(256, 768)
                                                                                    .build()),
    MULTILEVEL_LAST(MultiLevelLastTerrainGenerator::new, ParameterConstraints.ANY),
    ONE_ISLAND(OneIslandTerrainGenerator::new, ParameterConstraints.builder().mapSizes(384, 1024).build()),
    SMALL_ISLANDS(SmallIslandsTerrainGenerator::new, ParameterConstraints.builder()
                                                                         .mapSizes(768, 1024)
                                                                         .build()),
    VALLEY(ValleyTerrainGenerator::new, ParameterConstraints.builder()
                                                            .mapSizes(384, 1024)
                                                            .build()),
    VALLEY_LAST(ValleyLastTerrainGenerator::new, ParameterConstraints.builder()
                                                                     .mapSizes(512, 1024)
                                                                     .build()),
    RIVERS(RiversTerrainGenerator::new, ParameterConstraints.ANY),
    RIVERS_AND_OCEANS(RiversAndOceansTerrainGenerator::new, ParameterConstraints.ANY),
    FRACTAL_LAND(FractalLandLastTerrainGenerator::new, ParameterConstraints.ANY),
    FRACTAL_PLATEAU(FractalPlateauLastTerrainGenerator::new, ParameterConstraints.ANY),
    FRACTAL_NAVY(FractalNavyLastTerrainGenerator::new, ParameterConstraints.ANY),
    SETONS(SetonishLastTerrainGenerator::new, ParameterConstraints.ANY);

    private final Supplier<com.faforever.neroxis.generator.terrain.TerrainGenerator> generatorSupplier;
    private final ParameterConstraints parameterConstraints;

    @JsonIgnore
    @Override
    public ParameterConstraints parameterConstraints() {
        return parameterConstraints;
    }
}
