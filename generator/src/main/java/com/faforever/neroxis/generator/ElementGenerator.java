package com.faforever.neroxis.generator;

import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import java.util.Random;
import lombok.Getter;

@Getter
public abstract strictfp class ElementGenerator {

    protected SCMap map;
    protected Random random;
    protected GeneratorParameters generatorParameters;
    protected SymmetrySettings symmetrySettings;
    protected ParameterConstraints parameterConstraints = ParameterConstraints.builder().build();
    protected float weight = 1;

    public abstract void setupPipeline();

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        this.map = map;
        this.random = new Random(seed);
        this.generatorParameters = generatorParameters;
        this.symmetrySettings = symmetrySettings;
    }
}
