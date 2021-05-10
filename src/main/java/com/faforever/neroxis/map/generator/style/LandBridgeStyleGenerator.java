package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.prop.*;
import com.faforever.neroxis.map.generator.terrain.LandBridgeTerrainGenerator;

import java.util.Arrays;

public strictfp class LandBridgeStyleGenerator extends StyleGenerator {

    public LandBridgeStyleGenerator() {
        name = "LAND_BRIDGE";
        weight = 2;
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.25f, .75f)
                .mexDensity(.5f, 1f)
                .reclaimDensity(.5f, 1f)
                .mapSizes(1024)
                .numTeams(2, 4)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerator = new LandBridgeTerrainGenerator();
        propGenerators.addAll(Arrays.asList(new LargeBattlePropGenerator(), new NavyWrecksPropGenerator(),
                new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
        int mapSize = mapParameters.getMapSize();
        teamSeparation = mapSize / 2;
        spawnSeparation = mapSize / 8f;
    }
}

