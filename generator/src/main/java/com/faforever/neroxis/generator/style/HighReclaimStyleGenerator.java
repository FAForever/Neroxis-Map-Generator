package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.generator.terrain.DropPlateauTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MountainRangeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;
import com.faforever.neroxis.map.MapParameters;

import java.util.Arrays;

public strictfp class HighReclaimStyleGenerator extends StyleGenerator {

    public HighReclaimStyleGenerator() {
        name = "HIGH_RECLAIM";
        weight = .25f;
        parameterConstraints = ParameterConstraints.builder()
                .mountainDensity(.75f, 1f)
                .plateauDensity(.5f, 1f)
                .rampDensity(0f, .25f)
                .reclaimDensity(.8f, 1f)
                .biomes("Desert", "Frithen", "Loki", "Moonlight", "Wonder")
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerators.addAll(Arrays.asList(new DropPlateauTerrainGenerator(), new MountainRangeTerrainGenerator(),
                new LittleMountainTerrainGenerator(), new ValleyTerrainGenerator()));
        propGenerator = new HighReclaimPropGenerator();
    }
}


