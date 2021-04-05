package neroxis.generator.style;

import lombok.Getter;
import neroxis.generator.prop.*;
import neroxis.map.MapParameters;

import java.util.Arrays;

@Getter
public strictfp class BasicStyleGenerator extends StyleGenerator {

    public BasicStyleGenerator() {
        name = "BASIC";
    }

    @Override
    protected void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        propGenerators.addAll(Arrays.asList(new BasicPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}

