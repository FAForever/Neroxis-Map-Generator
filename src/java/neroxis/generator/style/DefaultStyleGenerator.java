package neroxis.generator.style;

import lombok.Getter;
import neroxis.generator.prop.*;
import neroxis.map.MapParameters;

import java.util.Arrays;

@Getter
public strictfp class DefaultStyleGenerator extends StyleGenerator {

    public DefaultStyleGenerator() {
        name = "DEFAULT";
    }

    @Override
    public void initialize(MapParameters mapParameters, long seed) {
        super.initialize(mapParameters, seed);
        propGenerators.addAll(Arrays.asList(new DefaultPropGenerator(), new EnemyCivPropGenerator(), new LargeBattlePropGenerator(),
                new NavyWrecksPropGenerator(), new NeutralCivPropGenerator(), new RockFieldPropGenerator(), new SmallBattlePropGenerator()));
    }
}

