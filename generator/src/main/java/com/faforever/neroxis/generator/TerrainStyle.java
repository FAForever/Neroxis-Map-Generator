package com.faforever.neroxis.generator;

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
import com.faforever.neroxis.generator.terrain.FractalUpsideDownLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LandBridgeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MountainRangeLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MountainRangeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MultiLevelLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.OneIslandTerrainGenerator;
import com.faforever.neroxis.generator.terrain.RiversAndOceansTerrainGenerator;
import com.faforever.neroxis.generator.terrain.RiversTerrainGenerator;
import com.faforever.neroxis.generator.terrain.SetonsLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.SmallIslandsTerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum TerrainStyle {
    BASIC(BasicTerrainGenerator::new),
    BASIC_LAST(BasicLastTerrainGenerator::new),
    SETONS(SetonsLastTerrainGenerator::new),
    BIG_ISLANDS(BigIslandsTerrainGenerator::new),
    CENTER_LAKE(CenterLakeTerrainGenerator::new),
    CENTER_LAKE_LAST(CenterLakeLastTerrainGenerator::new),
    DROP_PLATEAU(DropPlateauTerrainGenerator::new),
    DROP_PLATEAU_LAST(DropPlateauLastTerrainGenerator::new),
    FLOODED(FloodedTerrainGenerator::new),
    LAND_BRIDGE(LandBridgeTerrainGenerator::new),
    LITTLE_MOUNTAIN(LittleMountainTerrainGenerator::new),
    LITTLE_MOUNTAIN_LAST(LittleMountainLastTerrainGenerator::new),
    MOUNTAIN_RANGE(MountainRangeTerrainGenerator::new),
    MOUNTAIN_RANGE_LAST(MountainRangeLastTerrainGenerator::new),
    MULTILEVEL_LAST(MultiLevelLastTerrainGenerator::new),
    ONE_ISLAND(OneIslandTerrainGenerator::new),
    SMALL_ISLANDS(SmallIslandsTerrainGenerator::new),
    VALLEY(ValleyTerrainGenerator::new),
    VALLEY_LAST(ValleyLastTerrainGenerator::new),
    RIVERS(RiversTerrainGenerator::new),
    RIVERS_AND_OCEANS(RiversAndOceansTerrainGenerator::new),
    FRACTAL_LAND(FractalLandLastTerrainGenerator::new),
    FRACTAL_PLATEAU(FractalPlateauLastTerrainGenerator::new),
    FRACTAL_NAVY(FractalNavyLastTerrainGenerator::new),
    FRACTAL_UPSIDE_DOWN(FractalUpsideDownLastTerrainGenerator::new);

    private final Supplier<com.faforever.neroxis.generator.terrain.TerrainGenerator> generatorSupplier;
}
