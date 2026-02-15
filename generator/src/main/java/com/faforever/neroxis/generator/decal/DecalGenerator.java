package com.faforever.neroxis.generator.decal;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.DecalPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;

import java.util.Random;

public abstract class DecalGenerator implements HasParameterConstraints {
    protected SCMap map;
    protected Random random;
    protected GeneratorParameters generatorParameters;
    protected SymmetrySettings symmetrySettings;

    protected DecalPlacer decalPlacer;
    protected FloatMask slope;
    protected BooleanMask passableLand;
    protected BooleanMask fieldDecal;
    protected BooleanMask slopeDecal;

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        this.map = map;
        this.random = new Random(seed);
        this.generatorParameters = generatorParameters;
        this.symmetrySettings = symmetrySettings;
        this.passableLand = new BooleanMask(1, random.nextLong(), symmetrySettings, "passableLand");
        this.slope = new FloatMask(1, random.nextLong(), symmetrySettings, "passableLand");
        passableLand.init(terrainGenerator.getPassableLand());
        slope.init(terrainGenerator.getSlope());
        fieldDecal = new BooleanMask(1, random.nextLong(), symmetrySettings, "fieldDecal");
        slopeDecal = new BooleanMask(1, random.nextLong(), symmetrySettings, "slopeDecal");
        decalPlacer = new DecalPlacer(map, random.nextLong());
    }

    public abstract void setupPipeline();

    public abstract void placeDecals();
}
