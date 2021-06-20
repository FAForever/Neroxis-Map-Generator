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
        heightmap = terrainGenerator.getHeightmap().startVisualDebugger();
        slope = terrainGenerator.getSlope();
        normals = heightmap.copy().resample(512).addPerlinNoise(64, 12f).blur(1)
                .addGaussianNoise(.01f).getNormalMask(4f).startVisualDebugger();
    }

    public abstract void setTextures();

    public abstract void setCompressedNormal();

    public abstract void generatePreview();
}