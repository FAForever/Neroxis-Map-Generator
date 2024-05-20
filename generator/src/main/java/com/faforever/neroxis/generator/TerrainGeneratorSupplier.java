package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.terrain.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum TerrainGeneratorSupplier {
    BASIC(BasicTerrainGenerator.class, BasicTerrainGenerator::new),
    BIG_ISLANDS(BigIslandsTerrainGenerator.class, BigIslandsTerrainGenerator::new),
    CENTER_LAKE(CenterLakeTerrainGenerator.class, CenterLakeTerrainGenerator::new),
    DROP_PLATEAU(DropPlateauTerrainGenerator.class, DropPlateauTerrainGenerator::new),
    FLOODED(FloodedTerrainGenerator.class, FloodedTerrainGenerator::new),
    LAND_BRIDGE(LandBridgeTerrainGenerator.class, LandBridgeTerrainGenerator::new),
    LITTLE_MOUNTAIN(LittleMountainTerrainGenerator.class, LittleMountainTerrainGenerator::new),
    MOUNTAIN_RANGE(MountainRangeTerrainGenerator.class, MountainRangeTerrainGenerator::new),
    ONE_ISLAND(OneIslandTerrainGenerator.class, OneIslandTerrainGenerator::new),
    SMALL_ISLANDS(SmallIslandsTerrainGenerator.class, SmallIslandsTerrainGenerator::new),
    VALLEY(ValleyTerrainGenerator.class, ValleyTerrainGenerator::new);

    private final Class<? extends com.faforever.neroxis.generator.terrain.TerrainGenerator> generatorClass;
    private final Supplier<com.faforever.neroxis.generator.terrain.TerrainGenerator> generatorSupplier;
}
