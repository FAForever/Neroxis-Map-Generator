package com.faforever.neroxis.generator.texture;

import com.faforever.neroxis.generator.ElementGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.FloatMask;
import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import lombok.Getter;

@Getter
public abstract strictfp class TextureGenerator extends ElementGenerator {
    protected FloatMask heightmap;
    protected FloatMask slope;

    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters);
        this.heightmap = terrainGenerator.getHeightmap();
        this.slope = terrainGenerator.getSlope();
    }

    public abstract void setTextures();
}