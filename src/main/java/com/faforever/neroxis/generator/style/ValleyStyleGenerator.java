package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.*;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;
import com.faforever.neroxis.map.MapParameters;

import java.util.Arrays;

public strictfp class ValleyStyleGenerator extends StyleGenerator {

    public ValleyStyleGenerator() {
        name = "VALLEY";
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.75f, 1f)
                .mountainDensity(.5f, 1)
                .mapSizes(512, 1024)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        teamSeparation = mapParameters.getMapSize() / 2;
        terrainGenerator = new ValleyTerrainGenerator();
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator(), new HighReclaimPropGenerator()));
    }
}

