package neroxis.generator.style;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.prop.*;
import neroxis.generator.terrain.CenterLakeTerrainGenerator;
import neroxis.map.MapParameters;

import java.util.Arrays;

public strictfp class CenterLakeStyleGenerator extends StyleGenerator {

    public CenterLakeStyleGenerator() {
        name = "CENTER_LAKE";
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .5f)
                .plateauDensity(0, .5f)
                .mexDensity(.25f, 1)
                .mapSizes(512, 1024)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerators.add(new CenterLakeTerrainGenerator());
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(),
                new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }

}


