package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.prop.*;
import com.faforever.neroxis.map.generator.resource.LowMexResourceGenerator;
import com.faforever.neroxis.map.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.map.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.map.generator.terrain.OneIslandTerrainGenerator;
import com.faforever.neroxis.map.generator.terrain.ValleyTerrainGenerator;

import java.util.Arrays;

public strictfp class LowMexStyleGenerator extends StyleGenerator {

    public LowMexStyleGenerator() {
        name = "LOW_MEX";
        parameterConstraints = ParameterConstraints.builder()
                .mexDensity(0f, .25f)
                .mapSizes(256, 512)
                .spawnCount(0, 4)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        resourceGenerator = new LowMexResourceGenerator();
        terrainGenerators.addAll(Arrays.asList(new BasicTerrainGenerator(), new OneIslandTerrainGenerator(),
                new LittleMountainTerrainGenerator(), new ValleyTerrainGenerator()));
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator(),
                new HighReclaimPropGenerator()));
    }
}


