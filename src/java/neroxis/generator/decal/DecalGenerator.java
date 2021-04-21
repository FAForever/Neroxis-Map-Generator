package neroxis.generator.decal;

import neroxis.generator.ElementGenerator;
import neroxis.generator.placement.DecalPlacer;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.BinaryMask;
import neroxis.map.FloatMask;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;

public abstract strictfp class DecalGenerator extends ElementGenerator {
    protected DecalPlacer decalPlacer;
    protected FloatMask slope;
    protected BinaryMask passableLand;

    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters);
        this.slope = terrainGenerator.getSlope();
        this.passableLand = terrainGenerator.getPassableLand();
        decalPlacer = new DecalPlacer(map, random.nextLong());
    }

    public abstract void placeDecals();

}
