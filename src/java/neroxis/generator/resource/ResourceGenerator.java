package neroxis.generator.resource;

import neroxis.generator.ElementGenerator;
import neroxis.generator.HydroPlacer;
import neroxis.generator.MexPlacer;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;

public abstract strictfp class ResourceGenerator extends ElementGenerator {
    protected MexPlacer mexPlacer;
    protected HydroPlacer hydroPlacer;
    protected ConcurrentBinaryMask unbuildable;
    protected ConcurrentBinaryMask passableLand;

    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters);
        this.unbuildable = terrainGenerator.getUnbuildable();
        this.passableLand = terrainGenerator.getPassableLand();
        mexPlacer = new MexPlacer(map, random.nextLong());
        hydroPlacer = new HydroPlacer(map, random.nextLong());
    }

    public abstract void placeResources();

}
