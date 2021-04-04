package neroxis.generator.style;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.prop.*;
import neroxis.generator.terrain.LandBridgeTerrainGenerator;
import neroxis.map.MapParameters;

import java.util.Arrays;

public strictfp class LandBridgeStyleGenerator extends StyleGenerator {

    public LandBridgeStyleGenerator() {
        name = "LAND_BRIDGE";
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.25f, .75f)
                .mexDensity(.5f, 1f)
                .mapSizes(1024)
                .build();
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerators.add(new LandBridgeTerrainGenerator());
        propGenerators.addAll(Arrays.asList(new LargeBattlePropGenerator(), new NavyWrecksPropGenerator(),
                new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
        int mapSize = mapParameters.getMapSize();
        teamSeparation = mapSize / 2;
        spawnSeparation = mapSize / 8f;
    }
}

