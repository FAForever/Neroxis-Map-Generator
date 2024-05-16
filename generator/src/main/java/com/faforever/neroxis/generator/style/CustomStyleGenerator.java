package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.util.HasParameterConstraints;

import java.util.function.Predicate;

public class CustomStyleGenerator extends StyleGenerator {

    @Override
    public ParameterConstraints getParameterConstraints() {
        // actually need to combine the constraints from the different generators
        return terrainGenerator.getParameterConstraints();
    }

    @Override
    protected void chooseGenerators() {
        Predicate<HasParameterConstraints> constraintsMatchPredicate = hasConstraints -> hasConstraints.getParameterConstraints()
                .matches(
                        getGeneratorParameters());
        resourceGenerator = getGeneratorParameters().resourceGenerator();
        // ...

        textureGenerator = getTextureGeneratorOptions().select(random, constraintsMatchPredicate);
        decalGenerator = getDecalGeneratorOptions().select(random, constraintsMatchPredicate);
    }
}
