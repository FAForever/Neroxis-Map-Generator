package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.*;
import com.faforever.neroxis.generator.resource.LowMexResourceGenerator;
import com.faforever.neroxis.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.generator.terrain.OneIslandTerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;
import com.faforever.neroxis.map.MapParameters;

import java.util.Arrays;

public strictfp class LowMexStyleGenerator extends StyleGenerator {

    public LowMexStyleGenerator() {
        name = "LOW_MEX";
        parameterConstraints = ParameterConstraints.builder()
                .mexDensity(0f, .25f)
                .mapSizes(256, 512)
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


