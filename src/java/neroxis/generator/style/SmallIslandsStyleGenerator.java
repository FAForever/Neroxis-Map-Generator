package neroxis.generator.style;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.prop.BasicPropGenerator;
import neroxis.generator.prop.NavyWrecksPropGenerator;
import neroxis.generator.prop.RockFieldPropGenerator;
import neroxis.generator.prop.SmallBattlePropGenerator;
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

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        teamSeparation = mapParameters.getMapSize() / 2;
        terrainGenerator = new SmallIslandsTerrainGenerator();
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new NavyWrecksPropGenerator(),
                new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}


