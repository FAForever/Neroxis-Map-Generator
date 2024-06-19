package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.HydroPlacer;
import com.faforever.neroxis.map.placement.MexPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import lombok.Setter;

import java.util.Random;

public abstract class ResourceGenerator implements HasParameterConstraints {
    protected SCMap map;
    protected Random random;
    protected GeneratorParameters generatorParameters;
    protected SymmetrySettings symmetrySettings;

    protected MexPlacer mexPlacer;
    protected HydroPlacer hydroPlacer;
    protected BooleanMask unbuildable;
    protected BooleanMask passableLand;
    protected BooleanMask resourceMask;
    protected BooleanMask waterResourceMask;

    @Setter
    protected float resourceDensity = -1;

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        this.map = map;
        this.random = new Random(seed);
        this.generatorParameters = generatorParameters;
        this.symmetrySettings = symmetrySettings;
        this.unbuildable = terrainGenerator.getUnbuildable();
        this.passableLand = terrainGenerator.getPassableLand();
        resourceMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "resourceMask", true);
        waterResourceMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "waterResourceMask", true);
        mexPlacer = new MexPlacer(map, random.nextLong());
        hydroPlacer = new HydroPlacer(map, random.nextLong());

        if (resourceDensity == -1) {
            resourceDensity = random.nextFloat();
        }
    }

    public abstract void setupPipeline();

    public abstract void placeResources();

    protected abstract int getMexCount();
}
