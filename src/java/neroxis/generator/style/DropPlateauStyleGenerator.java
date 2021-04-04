package neroxis.generator.style;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.prop.*;
import neroxis.generator.terrain.DropPlateauTerrainGenerator;
import neroxis.map.MapParameters;

import java.util.Arrays;

public strictfp class DropPlateauStyleGenerator extends StyleGenerator {

    public DropPlateauStyleGenerator() {
        name = "DROP_PLATEAU";
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.5f, 1f)
                .plateauDensity(.5f, 1)
                .mexDensity(.25f, 1)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerators.add(new DropPlateauTerrainGenerator());
        propGenerators.addAll(Arrays.asList(new DefaultPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}


