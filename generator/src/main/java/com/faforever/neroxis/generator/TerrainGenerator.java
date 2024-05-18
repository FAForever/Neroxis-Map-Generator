package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.terrain.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum TerrainGenerator {
    BASIC(BasicTerrainGenerator.class, BasicTerrainGenerator::new, 1),
    BIG_ISLANDS(BigIslandsTerrainGenerator.class, BigIslandsTerrainGenerator::new, 1),
    CENTER_LAKE(CenterLakeTerrainGenerator.class, CenterLakeTerrainGenerator::new, 1),
    DROP_PLATEAU(DropPlateauTerrainGenerator.class, DropPlateauTerrainGenerator::new, 1),
    FLOODED(FloodedTerrainGenerator.class, FloodedTerrainGenerator::new, 1),
    LAND_BRIDGE(LandBridgeTerrainGenerator.class, LandBridgeTerrainGenerator::new, 1),
    LITTLE_MOUNTAIN(LittleMountainTerrainGenerator.class, LittleMountainTerrainGenerator::new, 1),
    MOUNTAIN_RANGE(MountainRangeTerrainGenerator.class, MountainRangeTerrainGenerator::new, 1),
    ONE_ISLAND(OneIslandTerrainGenerator.class, OneIslandTerrainGenerator::new, 1),
    SMALL_ISLANDS(SmallIslandsTerrainGenerator.class, SmallIslandsTerrainGenerator::new, 1),
    VALLEY(ValleyTerrainGenerator.class, ValleyTerrainGenerator::new, 1);

    private final Class<? extends com.faforever.neroxis.generator.terrain.TerrainGenerator> generatorClass;
    private final Supplier<com.faforever.neroxis.generator.terrain.TerrainGenerator> generatorSupplier;
    private final float weight;
}
