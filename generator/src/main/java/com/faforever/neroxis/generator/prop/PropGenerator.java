package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.PropPlacer;
import com.faforever.neroxis.map.placement.UnitPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import lombok.Getter;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Getter
public abstract class PropGenerator implements HasParameterConstraints {
    protected SCMap map;
    protected Random random;
    protected GeneratorParameters generatorParameters;
    protected SymmetrySettings symmetrySettings;

    protected UnitPlacer unitPlacer;
    protected PropPlacer propPlacer;
    protected BooleanMask impassable;
    protected BooleanMask unbuildable;
    protected BooleanMask passableLand;

    protected float reclaimDensity = -1;

    private CompletableFuture<Void> propsPlacedFuture;
    private CompletableFuture<Void> unitsPlacedFuture;

    public CompletableFuture<Void> getPropsPlacedFuture() {
        return propsPlacedFuture.copy();
    }

    public CompletableFuture<Void> getUnitsPlacedFuture() {
        return unitsPlacedFuture.copy();
    }

    public void setReclaimDensity(float reclaimDensity) {
        if (this.reclaimDensity != -1) {
            throw new IllegalStateException("resource density has already been set");
        }

        if (reclaimDensity < 0 || reclaimDensity > 1) {
            throw new IllegalArgumentException(
                    "reclaim density must be between 0 and 1, was %f".formatted(reclaimDensity));
        }

        this.reclaimDensity = reclaimDensity;
    }

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator, ResourceGenerator resourceGenerator) {
        this.map = map;
        this.random = new Random(seed);
        this.generatorParameters = generatorParameters;
        this.symmetrySettings = symmetrySettings;
        this.impassable = terrainGenerator.getImpassable();
        this.unbuildable = terrainGenerator.getUnbuildable();
        this.passableLand = terrainGenerator.getPassableLand();
        unitPlacer = new UnitPlacer(random.nextLong());
        propPlacer = new PropPlacer(map, random.nextLong());

        if (reclaimDensity == -1) {
            setReclaimDensity(random.nextFloat());
        }

        afterInitialize();

        propsPlacedFuture = resourceGenerator.getResourcesPlacedFuture().thenRunAsync(this::placeProps);
        unitsPlacedFuture = resourceGenerator.getResourcesPlacedFuture().thenRunAsync(this::placeUnits);

        setupPipeline();
    }

    protected void afterInitialize() {}

    protected abstract void setupPipeline();

    protected abstract void placeProps();

    protected abstract void placeUnits();
}
