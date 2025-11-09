package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.generator.prop.BoulderFieldPropGenerator;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.resource.HighMexLandLowMexWaterResourceGenerator;
import com.faforever.neroxis.generator.terrain.FractalNavyLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;

public class FractalNavyStyleGenerator extends StyleGenerator {
    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new HighMexLandLowMexWaterResourceGenerator());
    }

    @Override
    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new FractalNavyLastTerrainGenerator());
    }

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicPropGenerator(),
                                              new WeightedOption<>(new BasicPropGenerator(), 1f),
                                              new WeightedOption<>(new NavyWrecksPropGenerator(), 3f),
                                              new WeightedOption<>(new BoulderFieldPropGenerator(), 2f),
                                              new WeightedOption<>(new HighReclaimPropGenerator(), 1f),
                                              new WeightedOption<>(new LargeBattlePropGenerator(), .5f),
                                              new WeightedOption<>(new RockFieldPropGenerator(), 0.5f),
                                              new WeightedOption<>(new SmallBattlePropGenerator(), 0.5f));
    }
}
