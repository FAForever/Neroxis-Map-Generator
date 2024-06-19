package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.PropPlacer;
import com.faforever.neroxis.map.placement.UnitPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import lombok.Getter;
import lombok.Setter;

import java.util.Random;

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

    @Setter
    protected float reclaimDensity = -1;

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
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
            reclaimDensity = random.nextFloat();
        }
    }

    public abstract void setupPipeline();

    public abstract void placeProps();

    public abstract void placeUnits();
}
