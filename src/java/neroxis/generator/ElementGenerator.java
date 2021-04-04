package neroxis.generator;

import lombok.Getter;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;

import java.util.Random;

@Getter
public abstract strictfp class ElementGenerator {
    protected SCMap map;
    protected Random random;
    protected MapParameters mapParameters;

    protected ParameterConstraints parameterConstraints = ParameterConstraints.builder().build();
    protected float weight = 1;

    public abstract void setupPipeline();

    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        this.map = map;
        this.random = new Random(seed);
        this.mapParameters = mapParameters;
    }
}
