package com.faforever.neroxis.map.generator.decal;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.generator.ElementGenerator;
import com.faforever.neroxis.map.generator.placement.DecalPlacer;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.mask.BooleanMask;
import com.faforever.neroxis.map.mask.FloatMask;

public abstract strictfp class DecalGenerator extends ElementGenerator {
    protected DecalPlacer decalPlacer;
    protected FloatMask slope;
    protected BooleanMask passableLand;

    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters);
        this.slope = terrainGenerator.getSlope();
        this.passableLand = terrainGenerator.getPassableLand();
        decalPlacer = new DecalPlacer(map, random.nextLong());
    }

    public abstract void placeDecals();

}
