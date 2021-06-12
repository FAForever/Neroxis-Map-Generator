package com.faforever.neroxis.map.generator.style;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.prop.BasicPropGenerator;
import com.faforever.neroxis.map.generator.prop.EnemyCivPropGenerator;
import com.faforever.neroxis.map.generator.prop.HighReclaimPropGenerator;
import com.faforever.neroxis.map.generator.prop.LargeBattlePropGenerator;
import com.faforever.neroxis.map.generator.prop.NeutralCivPropGenerator;
import com.faforever.neroxis.map.generator.prop.RockFieldPropGenerator;
import com.faforever.neroxis.map.generator.prop.SmallBattlePropGenerator;
import com.faforever.neroxis.map.generator.terrain.DropPlateauTerrainGenerator;

import java.util.Arrays;

public strictfp class DropPlateauStyleGenerator extends StyleGenerator {

    public DropPlateauStyleGenerator() {
        name = "DROP_PLATEAU";
        weight = .5f;
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.5f, 1f)
                .plateauDensity(.5f, 1)
                .mexDensity(.25f, 1)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerator = new DropPlateauTerrainGenerator();
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator(), new HighReclaimPropGenerator()));
    }
}


