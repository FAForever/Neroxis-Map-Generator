package neroxis.generator.texture;

import lombok.Getter;
import neroxis.generator.ElementGenerator;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.ConcurrentFloatMask;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;

@Getter
public abstract strictfp class TextureGenerator extends ElementGenerator {
    protected ConcurrentFloatMask heightmap;
    protected ConcurrentFloatMask slope;

    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters);
        this.heightmap = terrainGenerator.getHeightmap();
        this.slope = terrainGenerator.getSlope();
    }

    public abstract void setTextures();
}