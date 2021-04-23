package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.map.generator.terrain.DropPlateauTerrainGenerator;
import com.faforever.neroxis.map.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.map.generator.terrain.MountainRangeTerrainGenerator;
import com.faforever.neroxis.map.generator.terrain.ValleyTerrainGenerator;

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


