package neroxis.generator.texture;

import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

public class BasicTextureGenerator extends TextureGenerator {
    protected ConcurrentBinaryMask realLand;
    protected ConcurrentBinaryMask realPlateaus;
    protected ConcurrentFloatMask accentGroundTexture;
    protected ConcurrentFloatMask waterBeachTexture;
    protected ConcurrentFloatMask accentSlopesTexture;
    protected ConcurrentFloatMask accentPlateauTexture;
    protected ConcurrentFloatMask slopesTexture;
    protected ConcurrentFloatMask steepHillsTexture;
    protected ConcurrentFloatMask rockTexture;
    protected ConcurrentFloatMask accentRockTexture;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        realLand = new ConcurrentBinaryMask(heightmap, mapParameters.getBiome().getWaterSettings().getElevation(), random.nextLong(), "realLand");
        realPlateaus = new ConcurrentBinaryMask(heightmap, mapParameters.getBiome().getWaterSettings().getElevation() + 3f, random.nextLong(), "realPlateaus");
        accentGroundTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentGroundTexture");
        waterBeachTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "waterBeachTexture");
        accentSlopesTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentSlopesTexture");
        accentPlateauTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentPlateauTexture");
        slopesTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "slopesTexture");
        steepHillsTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "steepHillsTexture");
        rockTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "rockTexture");
        accentRockTexture = new ConcurrentFloatMask(1, random.nextLong(), symmetrySettings, "accentRockTexture");
    }

    @Override
    public void setupPipeline() {
        if (mapParameters.getSymmetrySettings().getSpawnSymmetry().isPerfectSymmetry()) {
            setupTexturePipeline();
        } else {
            setupSimpleTexturePipeline();
        }
    }

    @Override
    public void setTextures() {
        Pipeline.await(accentGroundTexture, accentPlateauTexture, slopesTexture, accentSlopesTexture, steepHillsTexture, waterBeachTexture, rockTexture, accentRockTexture);
        Util.timedRun("neroxis.generator", "generateTextures", () -> {
            map.setTextureMasksLowScaled(accentGroundTexture.getFinalMask(), accentPlateauTexture.getFinalMask(), slopesTexture.getFinalMask(), accentSlopesTexture.getFinalMask());
            map.setTextureMasksHighScaled(steepHillsTexture.getFinalMask(), waterBeachTexture.getFinalMask(), rockTexture.getFinalMask(), accentRockTexture.getFinalMask());
        });
    }

    protected void setupTexturePipeline() {
        ConcurrentBinaryMask flat = new ConcurrentBinaryMask(slope, .05f, random.nextLong(), "flat").invert();
        ConcurrentBinaryMask accentGround = new ConcurrentBinaryMask(realLand, random.nextLong(), "accentGround");
        ConcurrentBinaryMask accentPlateau = new ConcurrentBinaryMask(realPlateaus, random.nextLong(), "accentPlateau");
        ConcurrentBinaryMask slopes = new ConcurrentBinaryMask(slope, .15f, random.nextLong(), "slopes");
        ConcurrentBinaryMask accentSlopes = new ConcurrentBinaryMask(slope, .55f, random.nextLong(), "accentSlopes").invert();
        ConcurrentBinaryMask steepHills = new ConcurrentBinaryMask(slope, .55f, random.nextLong(), "steepHills");
        ConcurrentBinaryMask rock = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "rock");
        ConcurrentBinaryMask accentRock = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "accentRock");

        accentGround.acid(.1f, 0).erode(.4f, SymmetryType.SPAWN).smooth(6, .75f);
        accentPlateau.acid(.1f, 0).erode(.4f, SymmetryType.SPAWN).smooth(6, .75f);
        slopes.flipValues(.95f).erode(.5f, SymmetryType.SPAWN).acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentSlopes.minus(flat).acid(.1f, 0).erode(.5f, SymmetryType.SPAWN).smooth(4, .75f).acid(.55f, 0);
        steepHills.acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentRock.acid(.2f, 0).erode(.3f, SymmetryType.SPAWN).acid(.2f, 0).smooth(2, .5f).intersect(rock);

        accentGroundTexture.init(accentGround, 0, .5f).smooth(12).add(accentGround, .325f).smooth(8).add(accentGround, .25f).clampMax(1f).smooth(2);
        accentPlateauTexture.init(accentPlateau, 0, .5f).smooth(12).add(accentPlateau, .325f).smooth(8).add(accentPlateau, .25f).clampMax(1f).smooth(2);
        slopesTexture.init(slopes, 0, 1).smooth(8).add(slopes, .75f).smooth(4).clampMax(1f);
        accentSlopesTexture.init(accentSlopes, 0, 1).smooth(8).add(accentSlopes, .65f).smooth(4).add(accentSlopes, .5f).smooth(1).clampMax(1f);
        steepHillsTexture.init(steepHills, 0, 1).smooth(8).clampMax(0.35f).add(steepHills, .65f).smooth(4).clampMax(0.65f).add(steepHills, .5f).smooth(1).clampMax(1f);
        waterBeachTexture.init(realLand.copy().invert().inflate(12).minus(realPlateaus), 0, 1).smooth(12);
        rockTexture.init(rock, 0, 1f).smooth(4).add(rock, 1f).smooth(2).clampMax(1f);
        accentRockTexture.init(accentRock, 0, 1f).smooth(4).clampMax(1f);
    }

    protected void setupSimpleTexturePipeline() {
        ConcurrentBinaryMask flat = new ConcurrentBinaryMask(slope, .05f, random.nextLong(), "flat").invert();
        ConcurrentBinaryMask accentGround = new ConcurrentBinaryMask(realLand, random.nextLong(), "accentGround");
        ConcurrentBinaryMask accentPlateau = new ConcurrentBinaryMask(realPlateaus, random.nextLong(), "accentPlateau");
        ConcurrentBinaryMask slopes = new ConcurrentBinaryMask(slope, .15f, random.nextLong(), "slopes");
        ConcurrentBinaryMask accentSlopes = new ConcurrentBinaryMask(slope, .55f, random.nextLong(), "accentSlopes").invert();
        ConcurrentBinaryMask steepHills = new ConcurrentBinaryMask(slope, .55f, random.nextLong(), "steepHills");
        ConcurrentBinaryMask rock = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "rock");
        ConcurrentBinaryMask accentRock = new ConcurrentBinaryMask(slope, .75f, random.nextLong(), "accentRock");

        accentSlopes.minus(flat);
        accentRock.intersect(rock);

        accentGroundTexture.init(accentGround, 0, .5f).smooth(12).add(accentGround, .325f).smooth(8).add(accentGround, .25f).clampMax(1f).smooth(2);
        accentPlateauTexture.init(accentPlateau, 0, .5f).smooth(12).add(accentPlateau, .325f).smooth(8).add(accentPlateau, .25f).clampMax(1f).smooth(2);
        slopesTexture.init(slopes, 0, 1).smooth(8).add(slopes, .75f).smooth(4).clampMax(1f);
        accentSlopesTexture.init(accentSlopes, 0, 1).smooth(8).add(accentSlopes, .65f).smooth(4).add(accentSlopes, .5f).smooth(1).clampMax(1f);
        steepHillsTexture.init(steepHills, 0, 1).smooth(8).clampMax(0.35f).add(steepHills, .65f).smooth(4).clampMax(0.65f).add(steepHills, .5f).smooth(1).clampMax(1f);
        waterBeachTexture.init(realLand.copy().invert().inflate(12).minus(realPlateaus), 0, 1).smooth(12);
        rockTexture.init(rock, 0, 1f).smooth(4).add(rock, 1f).smooth(2).clampMax(1f);
        accentRockTexture.init(accentRock, 0, 1f).smooth(4).clampMax(1f);
    }
}
