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
import com.faforever.neroxis.generator.style.TestStyleGenerator;
import com.faforever.neroxis.generator.style.ValleyStyleGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MapStyle {
    BASIC(BasicStyleGenerator.class, 1, true),
    BIG_ISLANDS(BigIslandsStyleGenerator.class, 4, true),
    CENTER_LAKE(CenterLakeStyleGenerator.class, 1, true),
    DROP_PLATEAU(DropPlateauStyleGenerator.class, .5f, true),
    FLOODED(FloodedStyleGenerator.class, .01f, true),
    HIGH_RECLAIM(HighReclaimStyleGenerator.class, .25f, true),
    LAND_BRIDGE(LandBridgeStyleGenerator.class, 2, true),
    LITTLE_MOUNTAIN(LittleMountainStyleGenerator.class, 1, true),
    LOW_MEX(LowMexStyleGenerator.class, .5f, true),
    MOUNTAIN_RANGE(MountainRangeStyleGenerator.class, 1, true),
    ONE_ISLAND(OneIslandStyleGenerator.class, 1, true),
    SMALL_ISLANDS(SmallIslandsStyleGenerator.class, 4, true),
    VALLEY(ValleyStyleGenerator.class, 1, true),
    TEST(TestStyleGenerator.class, 0, false);

    private final Class<? extends StyleGenerator> generatorClass;
    private final float weight;
    private final boolean production;
}
