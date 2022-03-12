package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.generator.ElementGenerator;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.placement.HydroPlacer;
import com.faforever.neroxis.map.placement.MexPlacer;
import com.faforever.neroxis.mask.BooleanMask;

public abstract strictfp class ResourceGenerator extends ElementGenerator {
    protected MexPlacer mexPlacer;
    protected HydroPlacer hydroPlacer;
    protected BooleanMask unbuildable;
    protected BooleanMask passableLand;

    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters);
        this.unbuildable = terrainGenerator.getUnbuildable();
        this.passableLand = terrainGenerator.getPassableLand();
        mexPlacer = new MexPlacer(map, random.nextLong());
        hydroPlacer = new HydroPlacer(map, random.nextLong());
    }

    public abstract void placeResources();

    protected abstract int getMexCount();

}
