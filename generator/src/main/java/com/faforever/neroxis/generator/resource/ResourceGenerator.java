package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.generator.util.serial.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.HydroPlacer;
import com.faforever.neroxis.map.placement.MexPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import lombok.Getter;

import java.util.random.RandomGenerator;

@Getter
public abstract class ResourceGenerator implements HasParameterConstraints {
    protected SCMap map;
    protected RandomGenerator.SplittableGenerator random;
    protected GeneratorParameters generatorParameters;
    protected SymmetrySettings symmetrySettings;

    protected MexPlacer mexPlacer;
    protected HydroPlacer hydroPlacer;
    protected BooleanMask unbuildable;
    protected BooleanMask passableLand;
    protected BooleanMask passableWater;
    protected BooleanMask resourceMask;
    protected BooleanMask waterResourceMask;
    protected BooleanMask mexDeadZone;

    protected float resourceDensity = -1;

    public void setResourceDensity(float resourceDensity) {
        if (this.resourceDensity != -1) {
            throw new IllegalStateException("resource density has already been set");
        }

        if (resourceDensity < 0 || resourceDensity > 1) {
            throw new IllegalArgumentException(
                    "resource density must be between 0 and 1, was %f".formatted(resourceDensity));
        }

        this.resourceDensity = resourceDensity;
    }

    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        this.map = map;
        this.random = random.split();
        this.generatorParameters = generatorParameters;
        this.symmetrySettings = symmetrySettings;
        this.passableWater = new BooleanMask(1, random.split(), symmetrySettings, "passableWater");
        this.unbuildable = new BooleanMask(1, random.split(), symmetrySettings, "unbuildable");
        this.passableLand = new BooleanMask(1, random.split(), symmetrySettings, "passableLand");
        resourceMask = new BooleanMask(1, random.split(), symmetrySettings, "resourceMask");
        waterResourceMask = new BooleanMask(1, random.split(), symmetrySettings, "waterResourceMask");
        mexDeadZone = new BooleanMask(1, random.split(), symmetrySettings, "mexDeadZone");
        passableWater.init(terrainGenerator.getPassableWater());
        unbuildable.init(terrainGenerator.getUnbuildable());
        passableLand.init(terrainGenerator.getPassableLand());
        mexDeadZone.init(terrainGenerator.getMexDeadZone());
        mexPlacer = new MexPlacer(map, random.split());
        hydroPlacer = new HydroPlacer(map, random.split());

        if (resourceDensity == -1) {
            setResourceDensity(random.nextFloat());
        }
    }

    public abstract void setupPipeline();

    public abstract void placeResources();

    protected abstract int getMexCount();
}
