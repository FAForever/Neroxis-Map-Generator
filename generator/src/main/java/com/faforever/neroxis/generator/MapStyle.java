package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.style.BasicStyleGenerator;
import com.faforever.neroxis.generator.style.BigIslandsLastStyleGenerator;
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
import com.faforever.neroxis.generator.style.TestStyleGenerator;
import com.faforever.neroxis.generator.style.ValleyStyleGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MapStyle {
    BASIC(BasicStyleGenerator.class, true),
    BIG_ISLANDS(BigIslandsStyleGenerator.class, true),
    BIG_ISLANDS_LAST(BigIslandsLastStyleGenerator.class, true),
    CENTER_LAKE(CenterLakeStyleGenerator.class, true),
    DROP_PLATEAU(DropPlateauStyleGenerator.class, true),
    FLOODED(FloodedStyleGenerator.class, true),
    HIGH_RECLAIM(HighReclaimStyleGenerator.class, true),
    LAND_BRIDGE(LandBridgeStyleGenerator.class, true),
    LITTLE_MOUNTAIN(LittleMountainStyleGenerator.class, true),
    LOW_MEX(LowMexStyleGenerator.class, true),
    MOUNTAIN_RANGE(MountainRangeStyleGenerator.class, true),
    ONE_ISLAND(OneIslandStyleGenerator.class, true),
    SMALL_ISLANDS(SmallIslandsStyleGenerator.class, true),
    VALLEY(ValleyStyleGenerator.class, true),
    TEST(TestStyleGenerator.class, false);

    private final Class<? extends StyleGenerator> generatorClass;
    private final boolean production;
}
