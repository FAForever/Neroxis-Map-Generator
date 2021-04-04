package neroxis.generator.style;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.prop.*;
import neroxis.generator.terrain.SmallIslandsTerrainGenerator;
import neroxis.map.MapParameters;

import java.util.Arrays;

public strictfp class SmallIslandsStyleGenerator extends StyleGenerator {

    public SmallIslandsStyleGenerator() {
        name = "SMALL_ISLANDS";
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .5f)
                .plateauDensity(0, .5f)
                .mexDensity(.5f, 1)
                .mapSizes(1024)
                .build();
    }

    public void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        teamSeparation = mapParameters.getMapSize() / 2;
        terrainGenerators.add(new SmallIslandsTerrainGenerator());
        propGenerators.addAll(Arrays.asList(new DefaultPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}


