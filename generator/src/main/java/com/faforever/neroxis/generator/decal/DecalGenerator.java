package com.faforever.neroxis.generator.decal;

import com.faforever.neroxis.generator.ElementGenerator;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.DecalPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;

public abstract strictfp class DecalGenerator extends ElementGenerator {
    protected DecalPlacer decalPlacer;
    protected FloatMask slope;
    protected BooleanMask passableLand;
    protected BooleanMask fieldDecal;
    protected BooleanMask slopeDecal;

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        this.slope = terrainGenerator.getSlope();
        this.passableLand = terrainGenerator.getPassableLand();
        fieldDecal = new BooleanMask(1, random.nextLong(), symmetrySettings, "fieldDecal", true);
        slopeDecal = new BooleanMask(1, random.nextLong(), symmetrySettings, "slopeDecal", true);
        decalPlacer = new DecalPlacer(map, random.nextLong());
    }

    public abstract void placeDecals();
}
