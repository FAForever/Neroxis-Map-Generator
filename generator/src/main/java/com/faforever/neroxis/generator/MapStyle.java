package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.style.BasicStyleGenerator;
import com.faforever.neroxis.generator.style.BigIslandsStyleGenerator;
import com.faforever.neroxis.generator.style.CenterLakeStyleGenerator;
import com.faforever.neroxis.generator.style.DropPlateauStyleGenerator;
import com.faforever.neroxis.generator.style.FloodedStyleGenerator;
import com.faforever.neroxis.generator.style.HighReclaimStyleGenerator;
import com.faforever.neroxis.generator.style.LandBridgeStyleGenerator;
import com.faforever.neroxis.generator.style.LittleMountainStyleGenerator;
import com.faforever.neroxis.generator.style.LowMexStyleGenerator;
import com.faforever.neroxis.generator.style.MountainRangeStyleGenerator;
import com.faforever.neroxis.generator.style.OneIslandStyleGenerator;
import com.faforever.neroxis.generator.style.SmallIslandsStyleGenerator;
import com.faforever.neroxis.generator.style.StyleGenerator;
import com.faforever.neroxis.generator.style.ValleyStyleGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum MapStyle {
    BASIC(BasicStyleGenerator.class, BasicStyleGenerator::new, 1),
    BIG_ISLANDS(BigIslandsStyleGenerator.class, BigIslandsStyleGenerator::new, 4),
    CENTER_LAKE(CenterLakeStyleGenerator.class, CenterLakeStyleGenerator::new, 1),
    DROP_PLATEAU(DropPlateauStyleGenerator.class, DropPlateauStyleGenerator::new, .5f),
    FLOODED(FloodedStyleGenerator.class, FloodedStyleGenerator::new, .01f),
    HIGH_RECLAIM(HighReclaimStyleGenerator.class, HighReclaimStyleGenerator::new, .25f),
    LAND_BRIDGE(LandBridgeStyleGenerator.class, LandBridgeStyleGenerator::new, 2),
    LITTLE_MOUNTAIN(LittleMountainStyleGenerator.class, LittleMountainStyleGenerator::new, 1),
    LOW_MEX(LowMexStyleGenerator.class, LowMexStyleGenerator::new, .5f),
    MOUNTAIN_RANGE(MountainRangeStyleGenerator.class, MountainRangeStyleGenerator::new, 1),
    ONE_ISLAND(OneIslandStyleGenerator.class, OneIslandStyleGenerator::new, 1),
    SMALL_ISLANDS(SmallIslandsStyleGenerator.class, SmallIslandsStyleGenerator::new, 4),
    VALLEY(ValleyStyleGenerator.class, ValleyStyleGenerator::new, 1);

    private final Class<? extends StyleGenerator> generatorClass;
    private final Supplier<StyleGenerator> generatorSupplier;
    private final float weight;
}
