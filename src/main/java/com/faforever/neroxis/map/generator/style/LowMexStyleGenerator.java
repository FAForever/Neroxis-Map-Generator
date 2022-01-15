package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.map.generator.prop.EnemyCivPropGenerator;
import com.faforever.neroxis.map.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.map.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.map.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.map.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.map.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.map.generator.prop.SmallBattlePropGenerator;
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
                .mapSizes(256, 640)
                .spawnCount(0, 4)
                .numTeams(2, 2)
                .build();
        weight = .5f;
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


