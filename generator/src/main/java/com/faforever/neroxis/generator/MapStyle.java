package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.style.BasicStyleGenerator;
import com.faforever.neroxis.generator.style.BigIslandsStyleGenerator;
import com.faforever.neroxis.generator.style.CenterLakeStyleGenerator;
import com.faforever.neroxis.generator.style.DropPlateauStyleGenerator;
import com.faforever.neroxis.generator.style.FloodedMultiLevelStyleGenerator;
import com.faforever.neroxis.generator.style.FloodedStyleGenerator;
import com.faforever.neroxis.generator.style.FractalNoiseStyleGenerator;
import com.faforever.neroxis.generator.style.HighReclaimStyleGenerator;
import com.faforever.neroxis.generator.style.LandBridgeStyleGenerator;
import com.faforever.neroxis.generator.style.LittleMountainStyleGenerator;
import com.faforever.neroxis.generator.style.LowMexStyleGenerator;
import com.faforever.neroxis.generator.style.MountainRangeStyleGenerator;
import com.faforever.neroxis.generator.style.MultiLevelStyleGenerator;
import com.faforever.neroxis.generator.style.OneIslandStyleGenerator;
import com.faforever.neroxis.generator.style.RiversAndOceansStyleGenerator;
import com.faforever.neroxis.generator.style.RiversStyleGenerator;
import com.faforever.neroxis.generator.style.SmallIslandsStyleGenerator;
import com.faforever.neroxis.generator.style.StyleGenerator;
import com.faforever.neroxis.generator.style.ValleyStyleGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum MapStyle {
    BASIC(BasicStyleGenerator::new, 1),
    BIG_ISLANDS(BigIslandsStyleGenerator::new, 1),
    CENTER_LAKE(CenterLakeStyleGenerator::new, 1),
    DROP_PLATEAU(DropPlateauStyleGenerator::new, .5f),
    FLOODED(FloodedStyleGenerator::new, .01f),
    HIGH_RECLAIM(HighReclaimStyleGenerator::new, .25f),
    LAND_BRIDGE(LandBridgeStyleGenerator::new, 2),
    LITTLE_MOUNTAIN(LittleMountainStyleGenerator::new, 1),
    LOW_MEX(LowMexStyleGenerator::new, .5f),
    MOUNTAIN_RANGE(MountainRangeStyleGenerator::new, 1),
    MULTILEVEL(MultiLevelStyleGenerator::new, 1f),
    FLOODED_MULTILEVEL(FloodedMultiLevelStyleGenerator::new, 0.75f),
    ONE_ISLAND(OneIslandStyleGenerator::new, 1),
    SMALL_ISLANDS(SmallIslandsStyleGenerator::new, 1),
    VALLEY(ValleyStyleGenerator::new, 1),
    RIVERS(RiversStyleGenerator::new, 0.25f),
    RIVERS_AND_OCEANS(RiversAndOceansStyleGenerator::new, 0.75f),
    FRACTAL(FractalNoiseStyleGenerator::new, 0.25f);

    private final Supplier<StyleGenerator> generatorSupplier;
    private final float weight;
}
