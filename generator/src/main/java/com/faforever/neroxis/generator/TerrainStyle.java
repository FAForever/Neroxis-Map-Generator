package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.generator.terrain.BigIslandsTerrainGenerator;
import com.faforever.neroxis.generator.terrain.CenterLakeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.DropPlateauTerrainGenerator;
import com.faforever.neroxis.generator.terrain.FloodedTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LandBridgeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MountainRangeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.OneIslandTerrainGenerator;
import com.faforever.neroxis.generator.terrain.SmallIslandsTerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum TerrainStyle {
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
