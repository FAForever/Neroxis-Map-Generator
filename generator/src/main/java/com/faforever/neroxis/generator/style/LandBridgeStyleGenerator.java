package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.generator.prop.NavyWrecksPropGenerator;
import com.faforever.neroxis.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.generator.terrain.LandBridgeTerrainGenerator;
import java.util.Arrays;

public strictfp class LandBridgeStyleGenerator extends StyleGenerator {

    public LandBridgeStyleGenerator() {
        weight = 2;
        parameterConstraints = ParameterConstraints.builder()
                                                   .landDensity(.25f, .75f)
                                                   .mexDensity(.5f, 1f)
                                                   .reclaimDensity(.5f, 1f)
                                                   .mapSizes(768, 1024)
                                                   .numTeams(2, 4)
                                                   .build();
    }

    @Override
    protected void initialize(GeneratorParameters generatorParameters, long seed) {
        super.initialize(generatorParameters, seed);
        terrainGenerator = new LandBridgeTerrainGenerator();
        propGenerators.addAll(Arrays.asList(new LargeBattlePropGenerator(), new NavyWrecksPropGenerator(),
                                            new NeutralCivPropGenerator(), new RockFieldPropGenerator(),
                                            new SmallBattlePropGenerator()));
        int mapSize = generatorParameters.getMapSize();
        spawnSeparation = mapSize / 8f;
    }
}

