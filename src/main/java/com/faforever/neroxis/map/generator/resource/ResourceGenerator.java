package com.faforever.neroxis.map.generator.resource;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.generator.ElementGenerator;
import com.faforever.neroxis.map.generator.placement.HydroPlacer;
import com.faforever.neroxis.map.generator.placement.MexPlacer;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.mask.BooleanMask;

public abstract strictfp class ResourceGenerator extends ElementGenerator {
    protected MexPlacer mexPlacer;
    protected HydroPlacer hydroPlacer;
    protected BooleanMask unbuildable;
    protected BooleanMask passableLand;

    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters);
        this.unbuildable = terrainGenerator.getUnbuildable();
        this.passableLand = terrainGenerator.getPassableLand();
        mexPlacer = new MexPlacer(map, random.nextLong());
        hydroPlacer = new HydroPlacer(map, random.nextLong());
    }

    public abstract void placeResources();

    protected abstract int getMexCount();

}
