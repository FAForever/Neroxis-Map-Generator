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
import com.faforever.neroxis.generator.terrain.SpawnLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum TerrainStyle {
    BASIC(BasicTerrainGenerator::new),
    SPAWN_LAST(SpawnLastTerrainGenerator::new),
    BIG_ISLANDS(BigIslandsTerrainGenerator::new),
    CENTER_LAKE(CenterLakeTerrainGenerator::new),
    DROP_PLATEAU(DropPlateauTerrainGenerator::new),
    FLOODED(FloodedTerrainGenerator::new),
    LAND_BRIDGE(LandBridgeTerrainGenerator::new),
    LITTLE_MOUNTAIN(LittleMountainTerrainGenerator::new),
    MOUNTAIN_RANGE(MountainRangeTerrainGenerator::new),
    ONE_ISLAND(OneIslandTerrainGenerator::new),
    SMALL_ISLANDS(SmallIslandsTerrainGenerator::new),
    VALLEY(ValleyTerrainGenerator::new);

    private final Supplier<TerrainGenerator> generatorSupplier;
}
