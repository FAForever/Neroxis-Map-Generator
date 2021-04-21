package neroxis.generator.texture;

import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

public class BasicTextureGenerator extends TextureGenerator {
    protected BinaryMask realLand;
    protected BinaryMask realPlateaus;
    protected FloatMask accentGroundTexture;
    protected FloatMask waterBeachTexture;
    protected FloatMask accentSlopesTexture;
    protected FloatMask accentPlateauTexture;
    protected FloatMask slopesTexture;
    protected FloatMask steepHillsTexture;
    protected FloatMask rockTexture;
    protected FloatMask accentRockTexture;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        realLand = new BinaryMask(heightmap, mapParameters.getBiome().getWaterSettings().getElevation(), random.nextLong(), "realLand");
        realPlateaus = new BinaryMask(heightmap, mapParameters.getBiome().getWaterSettings().getElevation() + 3f, random.nextLong(), "realPlateaus");
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
    }

    @Override
    public void setTextures() {
        Pipeline.await(accentGroundTexture, accentPlateauTexture, slopesTexture, accentSlopesTexture, steepHillsTexture, waterBeachTexture, rockTexture, accentRockTexture);
        Util.timedRun("neroxis.generator", "generateTextures", () -> {
            map.setTextureMasksScaled(map.getTextureMasksLow(), accentGroundTexture.getFinalMask(), accentPlateauTexture.getFinalMask(), slopesTexture.getFinalMask(), accentSlopesTexture.getFinalMask());
            map.setTextureMasksScaled(map.getTextureMasksHigh(), steepHillsTexture.getFinalMask(), waterBeachTexture.getFinalMask(), rockTexture.getFinalMask(), accentRockTexture.getFinalMask());
        });
    }

    protected void setupTexturePipeline() {
        BinaryMask flat = new BinaryMask(slope, .05f, random.nextLong(), "flat").invert();
        BinaryMask accentGround = new BinaryMask(realLand, random.nextLong(), "accentGround");
        BinaryMask accentPlateau = new BinaryMask(realPlateaus, random.nextLong(), "accentPlateau");
        BinaryMask slopes = new BinaryMask(slope, .15f, random.nextLong(), "slopes");
        BinaryMask accentSlopes = new BinaryMask(slope, .55f, random.nextLong(), "accentSlopes").invert();
        BinaryMask steepHills = new BinaryMask(slope, .55f, random.nextLong(), "steepHills");
        BinaryMask rock = new BinaryMask(slope, .75f, random.nextLong(), "rock");
        BinaryMask accentRock = new BinaryMask(slope, .75f, random.nextLong(), "accentRock");

        accentGround.acid(.1f, 0).erode(.4f, SymmetryType.SPAWN).blur(6, .75f);
        accentPlateau.acid(.1f, 0).erode(.4f, SymmetryType.SPAWN).blur(6, .75f);
        slopes.flipValues(.95f).erode(.5f, SymmetryType.SPAWN).acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentSlopes.minus(flat).acid(.1f, 0).erode(.5f, SymmetryType.SPAWN).blur(4, .75f).acid(.55f, 0);
        steepHills.acid(.3f, 0).erode(.2f, SymmetryType.SPAWN);
        accentRock.acid(.2f, 0).erode(.3f, SymmetryType.SPAWN).acid(.2f, 0).blur(2, .5f).intersect(rock);

        accentGroundTexture.init(accentGround, 0, .5f).blur(12).add(accentGround, .325f).blur(8).add(accentGround, .25f).clampMax(1f).blur(2);
        accentPlateauTexture.init(accentPlateau, 0, .5f).blur(12).add(accentPlateau, .325f).blur(8).add(accentPlateau, .25f).clampMax(1f).blur(2);
        slopesTexture.init(slopes, 0, 1).blur(8).add(slopes, .75f).blur(4).clampMax(1f);
        accentSlopesTexture.init(accentSlopes, 0, 1).blur(8).add(accentSlopes, .65f).blur(4).add(accentSlopes, .5f).blur(1).clampMax(1f);
        steepHillsTexture.init(steepHills, 0, 1).blur(8).clampMax(0.35f).add(steepHills, .65f).blur(4).clampMax(0.65f).add(steepHills, .5f).blur(1).clampMax(1f);
        waterBeachTexture.init(realLand.copy().invert().inflate(12).minus(realPlateaus), 0, 1).blur(12);
        rockTexture.init(rock, 0, 1f).blur(4).add(rock, 1f).blur(2).clampMax(1f);
        accentRockTexture.init(accentRock, 0, 1f).blur(4).clampMax(1f);
    }

    protected void setupSimpleTexturePipeline() {
        BinaryMask flat = new BinaryMask(slope, .05f, random.nextLong(), "flat").invert();
        BinaryMask accentGround = new BinaryMask(realLand, random.nextLong(), "accentGround");
        BinaryMask accentPlateau = new BinaryMask(realPlateaus, random.nextLong(), "accentPlateau");
        BinaryMask slopes = new BinaryMask(slope, .15f, random.nextLong(), "slopes");
        BinaryMask accentSlopes = new BinaryMask(slope, .55f, random.nextLong(), "accentSlopes").invert();
        BinaryMask steepHills = new BinaryMask(slope, .55f, random.nextLong(), "steepHills");
        BinaryMask rock = new BinaryMask(slope, .75f, random.nextLong(), "rock");
        BinaryMask accentRock = new BinaryMask(slope, .75f, random.nextLong(), "accentRock");

        accentSlopes.minus(flat);
        accentRock.intersect(rock);

        accentGroundTexture.init(accentGround, 0, .5f).blur(12).add(accentGround, .325f).blur(8).add(accentGround, .25f).clampMax(1f).blur(2);
        accentPlateauTexture.init(accentPlateau, 0, .5f).blur(12).add(accentPlateau, .325f).blur(8).add(accentPlateau, .25f).clampMax(1f).blur(2);
        slopesTexture.init(slopes, 0, 1).blur(8).add(slopes, .75f).blur(4).clampMax(1f);
        accentSlopesTexture.init(accentSlopes, 0, 1).blur(8).add(accentSlopes, .65f).blur(4).add(accentSlopes, .5f).blur(1).clampMax(1f);
        steepHillsTexture.init(steepHills, 0, 1).blur(8).clampMax(0.35f).add(steepHills, .65f).blur(4).clampMax(0.65f).add(steepHills, .5f).blur(1).clampMax(1f);
        waterBeachTexture.init(realLand.copy().invert().inflate(12).minus(realPlateaus), 0, 1).blur(12);
        rockTexture.init(rock, 0, 1f).blur(4).add(rock, 1f).blur(2).clampMax(1f);
        accentRockTexture.init(accentRock, 0, 1f).blur(4).clampMax(1f);
    }
}
