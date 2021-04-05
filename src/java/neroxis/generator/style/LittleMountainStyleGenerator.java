package neroxis.generator.style;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.prop.*;
import neroxis.generator.terrain.LittleMountainTerrainGenerator;
import neroxis.map.MapParameters;

import java.util.Arrays;

public strictfp class LittleMountainStyleGenerator extends StyleGenerator {

    public LittleMountainStyleGenerator() {
        name = "LITTLE_MOUNTAIN";
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.5f, 1f)
                .mountainDensity(.25f, 1)
                .plateauDensity(0, .5f)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerators.add(new LittleMountainTerrainGenerator());
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator(), new HighReclaimPropGenerator()));
    }
}
