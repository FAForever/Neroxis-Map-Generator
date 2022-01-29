package com.faforever.neroxis.map.generator.texture;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.generator.ElementGenerator;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.NormalMask;
import lombok.Getter;

@Getter
public abstract strictfp class TextureGenerator extends ElementGenerator {
    protected FloatMask heightmap;
    protected FloatMask slope;
    protected NormalMask normals;
    protected FloatMask shadows;
    protected BooleanMask shadowsMask;

    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters);
        heightmap = terrainGenerator.getHeightmap();
        slope = terrainGenerator.getSlope();
        normals = heightmap.copy().resample(512).addPerlinNoise(64, 12f)
                .addGaussianNoise(.025f).blur(1).copyAsNormalMask(2f);
        shadowsMask = heightmap.copy().resample(512).copyAsShadowMask(mapParameters.getBiome().getLightingSettings().getSunDirection());
        shadows = shadowsMask.copyAsFloatMask(0, 1).blur(2);
    }

    public abstract void setTextures();

    public abstract void setCompressedDecals();

    public abstract void generatePreview();
}