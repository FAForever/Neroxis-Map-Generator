package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.prop.*;
import com.faforever.neroxis.map.generator.terrain.MountainRangeTerrainGenerator;

import java.util.Arrays;

public strictfp class MountainRangeStyleGenerator extends StyleGenerator {

    public MountainRangeStyleGenerator() {
        name = "MOUNTAIN_RANGE";
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.75f, 1f)
                .mountainDensity(.5f, 1)
                .plateauDensity(0, .5f)
                .mexDensity(.375f, 1)
                .mapSizes(256, 512)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerator = new MountainRangeTerrainGenerator();
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator(), new HighReclaimPropGenerator()));
    }
}
