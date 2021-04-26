package com.faforever.neroxis.map.generator.texture;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.generator.ElementGenerator;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.mask.FloatMask;
import com.faforever.neroxis.map.mask.NormalMask;
import lombok.Getter;

@Getter
public abstract strictfp class TextureGenerator extends ElementGenerator {
    protected FloatMask heightmap;
    protected FloatMask slope;
    protected NormalMask normals;

    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters);
        heightmap = terrainGenerator.getHeightmap();
        slope = terrainGenerator.getSlope();
        normals = heightmap.copy().waterErode(10000, 100, .9f, 10, .25f, .25f, 5, .5f)
                .getNormalMask(4f);
    }

    public abstract void setTextures();
}