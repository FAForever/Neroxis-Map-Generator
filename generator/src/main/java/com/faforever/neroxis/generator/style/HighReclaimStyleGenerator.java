package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorOptions;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.generator.terrain.DropPlateauTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MountainRangeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;

import java.util.List;

public class HighReclaimStyleGenerator extends StyleGenerator {
    public HighReclaimStyleGenerator() {
        weight = .25f;
    }

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder()
                                   .mountainDensity(.75f, 1f)
                                   .plateauDensity(.5f, 1f)
                                   .rampDensity(0f, .25f)
                                   .reclaimDensity(.8f, 1f)
                                   .biomes("Desert", "Frithen", "Loki", "Moonlight", "Wonder")
                                   .build();
    }

    @Override
    protected GeneratorOptions<TerrainGenerator> getTerrainGeneratorOptions() {
        return new GeneratorOptions<>(new BasicTerrainGenerator(),
                                      List.of(new DropPlateauTerrainGenerator(), new MountainRangeTerrainGenerator(),
                                              new LittleMountainTerrainGenerator(), new ValleyTerrainGenerator()));
    }

    @Override
    protected GeneratorOptions<PropGenerator> getPropGeneratorOptions() {
        return new GeneratorOptions<>(new HighReclaimPropGenerator());
    }
}


