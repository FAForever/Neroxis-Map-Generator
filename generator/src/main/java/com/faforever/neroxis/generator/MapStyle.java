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

@Getter
@AllArgsConstructor
public enum MapStyle {
    BASIC(BasicStyleGenerator.class, 1),
    BIG_ISLANDS(BigIslandsStyleGenerator.class, 4),
    CENTER_LAKE(CenterLakeStyleGenerator.class, 1),
    DROP_PLATEAU(DropPlateauStyleGenerator.class, .5f),
    FLOODED(FloodedStyleGenerator.class, 0),
    HIGH_RECLAIM(HighReclaimStyleGenerator.class, 1),
    LAND_BRIDGE(LandBridgeStyleGenerator.class, 2),
    LITTLE_MOUNTAIN(LittleMountainStyleGenerator.class, 1),
    LOW_MEX(LowMexStyleGenerator.class, .5f),
    MOUNTAIN_RANGE(MountainRangeStyleGenerator.class, 1),
    ONE_ISLAND(OneIslandStyleGenerator.class, 1),
    SMALL_ISLANDS(SmallIslandsStyleGenerator.class, 4),
    VALLEY(ValleyStyleGenerator.class, 1);

    private final Class<? extends StyleGenerator> generatorClass;
    private final float weight;
}
