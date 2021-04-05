package neroxis.generator.style;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.prop.*;
import neroxis.generator.terrain.OneIslandTerrainGenerator;
import neroxis.map.MapParameters;

import java.util.Arrays;

public strictfp class OneIslandStyleGenerator extends StyleGenerator {

    public OneIslandStyleGenerator() {
        name = "ONE_ISLAND";
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .75f)
                .mapSizes(512, 1024)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerators.add(new OneIslandTerrainGenerator());
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new NavyWrecksPropGenerator(),
                new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}


