package com.faforever.neroxis.map.generator.texture;

import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.map.generator.PreviewGenerator;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.mask.BooleanMask;
import com.faforever.neroxis.map.mask.FloatMask;
import com.faforever.neroxis.util.ImageUtils;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.Util;

import java.io.IOException;

public class BasicTextureGenerator extends TextureGenerator {
    protected BooleanMask realLand;
    protected BooleanMask realPlateaus;
    protected FloatMask accentGroundTexture;
    protected FloatMask waterBeachTexture;
    protected FloatMask accentSlopesTexture;
    protected FloatMask accentPlateauTexture;
    protected FloatMask slopesTexture;
    protected FloatMask steepHillsTexture;
    protected FloatMask rockTexture;
    protected FloatMask accentRockTexture;
    protected FloatMask accentGroundPreviewTexture;
    protected FloatMask waterBeachPreviewTexture;
    protected FloatMask accentSlopesPreviewTexture;
    protected FloatMask accentPlateauPreviewTexture;
    protected FloatMask slopesPreviewTexture;
    protected FloatMask steepHillsPreviewTexture;
    protected FloatMask rockPreviewTexture;
    protected FloatMask accentRockPreviewTexture;
    protected FloatMask heightmapPreview;
    protected FloatMask reflectance;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        realLand = new BooleanMask(heightmap, mapParameters.getBiome().getWaterSettings().getElevation(), random.nextLong(), "realLand");
        realPlateaus = new BooleanMask(heightmap, mapParameters.getBiome().getWaterSettings().getElevation() + 3f, random.nextLong(), "realPlateaus");
        accentGroundTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentGroundTexture", true);
        waterBeachTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "waterBeachTexture", true);
        accentSlopesTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentSlopesTexture", true);
        accentPlateauTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentPlateauTexture", true);
        slopesTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "slopesTexture", true);
        steepHillsTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "steepHillsTexture", true);
        rockTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "rockTexture", true);
        accentRockTexture = new FloatMask(1, random.nextLong(), symmetrySettings, "accentRockTexture", true);
    }

    @Override
    public void setupPipeline() {
        if (mapParameters.getSymmetrySettings().getSpawnSymmetry().isPerfectSymmetry()) {
            setupTexturePipeline();
        } else {
            setupSimpleTexturePipeline();
        }
        setupPreviewPipeline();
    }

    @Override
    public void setTextures() {
        Pipeline.await(accentGroundTexture, accentPlateauTexture, slopesTexture, accentSlopesTexture, steepHillsTexture, waterBeachTexture, rockTexture, accentRockTexture, normals);
        Util.timedRun("com.faforever.neroxis.map.generator", "generateTextures", () -> {
            map.setTextureMasksScaled(map.getTextureMasksLow(), accentGroundTexture.getFinalMask(), accentPlateauTexture.getFinalMask(), slopesTexture.getFinalMask(), accentSlopesTexture.getFinalMask());
            map.setTextureMasksScaled(map.getTextureMasksHigh(), steepHillsTexture.getFinalMask(), waterBeachTexture.getFinalMask(), rockTexture.getFinalMask(), accentRockTexture.getFinalMask());
            map.setCompressedNormal(ImageUtils.compressNormal(normals.getFinalMask()));
        });

    }

    @Override
    public void generatePreview() {
        Pipeline.await(accentGroundPreviewTexture, accentPlateauPreviewTexture, slopesPreviewTexture,
                accentSlopesPreviewTexture, steepHillsPreviewTexture, waterBeachPreviewTexture, rockPreviewTexture,
                accentRockPreviewTexture, reflectance, heightmapPreview);
        Util.timedRun("com.faforever.neroxis.map.generator", "generatePreview", () -> {
            if (!mapParameters.isBlind()) {
                PreviewGenerator.generatePreview(heightmapPreview.getFinalMask(), reflectance.getFinalMask(), map,
                        accentGroundPreviewTexture.getFinalMask(), accentPlateauPreviewTexture.getFinalMask(), slopesPreviewTexture.getFinalMask(), accentSlopesPreviewTexture.getFinalMask(),
                        steepHillsPreviewTexture.getFinalMask(), waterBeachPreviewTexture.getFinalMask(), rockPreviewTexture.getFinalMask(), accentRockPreviewTexture.getFinalMask());
            } else {
                try {
                    PreviewGenerator.generateBlankPreview(map);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected void setupTexturePipeline() {
        BooleanMask flat = new BooleanMask(slope, .05f, random.nextLong(), "flat").invert();
        BooleanMask accentGround = new BooleanMask(realLand, random.nextLong(), "accentGround");
        BooleanMask accentPlateau = new BooleanMask(realPlateaus, random.nextLong(), "accentPlateau");
        BooleanMask slopes = new BooleanMask(slope, .15f, random.nextLong(), "slopes");
        BooleanMask accentSlopes = new BooleanMask(slope, .55f, random.nextLong(), "accentSlopes").invert();
        BooleanMask steepHills = new BooleanMask(slope, .55f, random.nextLong(), "steepHills");
        BooleanMask rock = new BooleanMask(slope, .75f, random.nextLong(), "rock");
        BooleanMask accentRock = new BooleanMask(slope, .75f, random.nextLong(), "accentRock");

        accentGround.acid(.1f, 0).erode(.4f, SymmetryType.SPAWN).blur(6, .75f);
        accentPlateau.acid(.1f, 0).erode(.4f, SymmetryType.SPAWN).blur(6, .75f);
        slopes.flipValues(.95f).erode(.5f, SymmetryType.SPAWN).acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentSlopes.minus(flat).acid(.1f, 0).erode(.5f, SymmetryType.SPAWN).blur(4, .75f).acid(.55f, 0);
        steepHills.acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentRock.acid(.2f, 0).erode(.3f, SymmetryType.SPAWN).acid(.2f, 0).blur(2, .5f).intersect(rock);

        accentGroundTexture.init(accentGround, 0f, .5f).blur(12).add(accentGround, .325f).blur(8).add(accentGround, .25f).clampMax(1f).blur(2);
        accentPlateauTexture.init(accentPlateau, 0f, .5f).blur(12).add(accentPlateau, .325f).blur(8).add(accentPlateau, .25f).clampMax(1f).blur(2);
        slopesTexture.init(slopes, 0f, 1f).blur(8).add(slopes, .75f).blur(4).clampMax(1f);
        accentSlopesTexture.init(accentSlopes, 0f, 1f).blur(8).add(accentSlopes, .65f).blur(4).add(accentSlopes, .5f).blur(1).clampMax(1f);
        steepHillsTexture.init(steepHills, 0f, 1f).blur(8).clampMax(0.35f).add(steepHills, .65f).blur(4).clampMax(0.65f).add(steepHills, .5f).blur(1).clampMax(1f);
        waterBeachTexture.init(realLand.copy().invert().inflate(12).minus(realPlateaus), 0f, 1f).blur(12);
        rockTexture.init(rock, 0f, 1f).blur(4).add(rock, 1f).blur(2).clampMax(1f);
        accentRockTexture.init(accentRock, 0f, 1f).blur(4).clampMax(1f);
    }

    protected void setupSimpleTexturePipeline() {
        BooleanMask flat = new BooleanMask(slope, .05f, random.nextLong(), "flat").invert();
        BooleanMask accentGround = new BooleanMask(realLand, random.nextLong(), "accentGround");
        BooleanMask accentPlateau = new BooleanMask(realPlateaus, random.nextLong(), "accentPlateau");
        BooleanMask slopes = new BooleanMask(slope, .15f, random.nextLong(), "slopes");
        BooleanMask accentSlopes = new BooleanMask(slope, .55f, random.nextLong(), "accentSlopes").invert();
        BooleanMask steepHills = new BooleanMask(slope, .55f, random.nextLong(), "steepHills");
        BooleanMask rock = new BooleanMask(slope, .75f, random.nextLong(), "rock");
        BooleanMask accentRock = new BooleanMask(slope, .75f, random.nextLong(), "accentRock");

        accentSlopes.minus(flat);
        accentRock.intersect(rock);

        accentGroundTexture.init(accentGround, 0f, .5f).blur(12).add(accentGround, .325f).blur(8).add(accentGround, .25f).clampMax(1f).blur(2);
        accentPlateauTexture.init(accentPlateau, 0f, .5f).blur(12).add(accentPlateau, .325f).blur(8).add(accentPlateau, .25f).clampMax(1f).blur(2);
        slopesTexture.init(slopes, 0f, 1f).blur(8).add(slopes, .75f).blur(4).clampMax(1f);
        accentSlopesTexture.init(accentSlopes, 0f, 1f).blur(8).add(accentSlopes, .65f).blur(4).add(accentSlopes, .5f).blur(1).clampMax(1f);
        steepHillsTexture.init(steepHills, 0f, 1f).blur(8).clampMax(0.35f).add(steepHills, .65f).blur(4).clampMax(0.65f).add(steepHills, .5f).blur(1).clampMax(1f);
        waterBeachTexture.init(realLand.copy().invert().inflate(12).minus(realPlateaus), 0f, 1f).blur(12);
        rockTexture.init(rock, 0f, 1f).blur(4).add(rock, 1f).blur(2).clampMax(1f);
        accentRockTexture.init(accentRock, 0f, 1f).blur(4).clampMax(1f);
    }

    protected void setupPreviewPipeline() {
        accentGroundPreviewTexture = accentGroundTexture.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        accentPlateauPreviewTexture = accentPlateauTexture.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        slopesPreviewTexture = slopesTexture.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        accentSlopesPreviewTexture = accentSlopesTexture.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        steepHillsPreviewTexture = steepHillsTexture.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        waterBeachPreviewTexture = waterBeachTexture.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        rockPreviewTexture = rockTexture.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        accentRockPreviewTexture = accentRockTexture.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        heightmapPreview = heightmap.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        reflectance = normals.copy().resample(PreviewGenerator.PREVIEW_SIZE).dot(map.getBiome().getLightingSettings().getSunDirection()).add(1f).divide(2f);
    }
}
