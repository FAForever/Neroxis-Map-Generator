package neroxis.generator.style;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.prop.*;
import neroxis.generator.terrain.ValleyTerrainGenerator;
import neroxis.map.MapParameters;

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

    public void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        teamSeparation = mapParameters.getMapSize() / 2;
        terrainGenerators.add(new ValleyTerrainGenerator());
        propGenerators.addAll(Arrays.asList(new DefaultPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}

