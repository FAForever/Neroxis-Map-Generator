package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedConstrainedOptions;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.generator.terrain.DropPlateauTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MountainRangeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;

public class HighReclaimStyleGenerator extends StyleGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mountainDensity(.75f, 1f)
                                   .plateauDensity(.5f, 1f)
                                   .rampDensity(0f, .25f)
                                   .reclaimDensity(.8f, 1f)
                                   .biomes("Desert", "Frithen", "Moonlight", "Sunset", "Wonder")
                                   .build();
    }

    @Override
    protected WeightedConstrainedOptions<TerrainGenerator> getTerrainGeneratorOptions() {
        return new WeightedConstrainedOptions<>(new BasicTerrainGenerator(),
                                                new WeightedOption<>(new DropPlateauTerrainGenerator(), 1f),
                                                new WeightedOption<>(new MountainRangeTerrainGenerator(), 1f),
                                                new WeightedOption<>(new LittleMountainTerrainGenerator(), 1f),
                                                new WeightedOption<>(new ValleyTerrainGenerator(), 1f));
    }

    @Override
    protected WeightedConstrainedOptions<PropGenerator> getPropGeneratorOptions() {
        return WeightedConstrainedOptions.single(new HighReclaimPropGenerator());
    }
}


