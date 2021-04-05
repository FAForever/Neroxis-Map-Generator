package neroxis.generator.style;

import lombok.Getter;
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
    }
}

