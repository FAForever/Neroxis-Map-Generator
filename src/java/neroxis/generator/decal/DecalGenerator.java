package neroxis.generator.decal;

import neroxis.generator.DecalPlacer;
import neroxis.generator.ElementGenerator;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.ConcurrentFloatMask;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;

public abstract strictfp class DecalGenerator extends ElementGenerator {
    protected DecalPlacer decalPlacer;
    protected ConcurrentFloatMask slope;
    protected ConcurrentBinaryMask passableLand;

    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters);
        this.slope = terrainGenerator.getSlope();
        this.passableLand = terrainGenerator.getPassableLand();
        decalPlacer = new DecalPlacer(map, random.nextLong());
    }

    public abstract void placeDecals();

}
