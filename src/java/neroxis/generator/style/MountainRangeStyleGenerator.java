package neroxis.generator.style;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.prop.*;
import neroxis.generator.terrain.MountainRangeTerrainGenerator;
import neroxis.map.MapParameters;

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
        terrainGenerators.add(new MountainRangeTerrainGenerator());
        propGenerators.addAll(Arrays.asList(new DefaultPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}
