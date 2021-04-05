package neroxis.generator.style;

import lombok.Getter;
import neroxis.generator.prop.HighReclaimPropGenerator;
import neroxis.generator.terrain.DropPlateauTerrainGenerator;
import neroxis.map.MapParameters;

@Getter
public strictfp class TestStyleGenerator extends StyleGenerator {

    public TestStyleGenerator() {
        name = "TEST";
        weight = 0;
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        terrainGenerator = new DropPlateauTerrainGenerator();
        propGenerator = new HighReclaimPropGenerator();
        try {
            this.mapParameters = propGenerator.getParameterConstraints().mapToLevel(1f, mapParameters, random);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

