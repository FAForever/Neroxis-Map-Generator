package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.util.Pipeline;

import java.util.ArrayList;
import java.util.Set;

public class FractalNoiseLastTerrainGenerator extends MultiLevelLastTerrainGenerator {

    private record FlattenParams(
            float minHeight,
            float maxHeight,
            float destinationMinHeight,
            float destinationMaxHeight,
            float slope,
            int blurAmount,
            boolean hasRamps,
            boolean spawnable,
            float spawnMaskDeflate
    ) {}

    private record FractalType(
            float waterHeight,
            boolean useRandomWaterMask,
            int noiseMapBlurAmount,
            float noiseOctaveMultiplier,
            float noiseExpMultiplier,
            float minSpawnable,
            float maxSpawnable,
            int teamSeparation,
            FlattenParams[] flattenParams
    ) {}

    private BooleanMask symmetryLines;
    private FloatMask symmetryCliffs;
    private FloatMask rampNoise;

    private FractalType fractalType;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
        pipeline.setDebug(true);

        symmetryLines = new BooleanMask(1, random.nextLong(), symmetrySettings, "symmetryLines", pipeline);
        symmetryCliffs = new FloatMask(1, random.nextLong(), symmetrySettings, "symmetryCliffs", pipeline);
        rampNoise = new FloatMask(1, random.nextLong(), symmetrySettings, "rampNoise", pipeline);

        ArrayList<FractalType> FRACTAL_TYPES = new ArrayList<FractalType>();
        // Basic Land
        FRACTAL_TYPES.add(new FractalType(0f, false, 1, 1.5f, 8, 0, 1, 2, new FlattenParams[]{
                new FlattenParams(1, 4, 1, 1, 0, 0, false, false, 4),
                new FlattenParams(4, 6, 13, 13, 0, 2, false, false, 4),
                new FlattenParams(6, 15, 11, 11, 0, 0, false, false, 4),
                new FlattenParams(15, 22, 16, 16, 0, 1, false, false, 4),
        }));
        // Plateau's
        FRACTAL_TYPES.add(new FractalType(3f, false, 4, 1.2f, 4, 0, 0, 2, new FlattenParams[]{
                new FlattenParams(0f, 0.1f, 0, 4, 0.5f, 0, false, false, 4),
                new FlattenParams(0.1f, 1.0f, 4, 15, 2, 0, true, false, 4),
                new FlattenParams(1.0f, 27, 15, 15, 0, 0, false, true, 4),
                new FlattenParams(27, 50, 24, 24, 0, 2, false, false, 4),
        }));
        // Navy
        FRACTAL_TYPES.add(new FractalType(16, true, 2, 1.5f, 6, 0, 0, 3, new FlattenParams[]{
                new FlattenParams(0f, 1.0f, 0, 8, 0.25f, 0, false, false, 4),
                new FlattenParams(1.0f, 3f, 8, 16, 1f, 0, true, false, 4),
                new FlattenParams(3f, 27, 18, 18, 0, 1, false, true, 8),
                new FlattenParams(27, 50, 18, 35, 1, 1, false, false, 4),
        }));
        // The Upside - Down
        FRACTAL_TYPES.add(new FractalType(2, false, 2, 1.2f, 3, 0, 0, 3, new FlattenParams[]{
                new FlattenParams(0f, 0.8f, 0, 10, 0.5f, 0, false, false, 4),
                new FlattenParams(0.8f, 15f, 16, 16, 0, 1, true, true, 4),
                new FlattenParams(15f, 50f, 13, 12.5f, 0.5f, 1, false, false, 4),
        }));




        fractalType = FRACTAL_TYPES.get(random.nextInt(FRACTAL_TYPES.size()));
        //fractalType = FRACTAL_TYPES.get(2);
        System.out.println("FractalType : " + fractalType);
        if (fractalType.useRandomWaterMask) {
            waterMask = WaterMasks.values()[random.nextInt(WaterMasks.values().length)];
            System.out.println("WaterMask: " + waterMask.name());
        }
        System.out.println(fractalType);
        for (FlattenParams fp : fractalType.flattenParams) {
            System.out.println("FlattenParam: " + fp);
        }

        noiseSmallestDetail = 2;

        noiseOctaveMultiplier = fractalType.noiseOctaveMultiplier;

        noiseMapBlurAmount = fractalType.noiseMapBlurAmount;
        noiseScaleMaxToValue = 50;

        mountainBrushSize = 24;
        mountainBrushDensity = 8f;
        mountainBrushIntensity = 3f;

        waterHeight -= fractalType.waterHeight;

        symmetryLines.setSize(map.getSize() + 1);
        symmetryLines.drawSymmetryLines();
    }

    @Override
    protected void landSetup() {
        super.landSetup();

        landNoiseMap.scaleToNewMinAndMaxHeight(0, 1);
        landNoiseMap.scaleExponentially(fractalType.noiseExpMultiplier);
        landNoiseMap.scaleToNewMinAndMaxHeight(0, noiseScaleMaxToValue);
    }

    @Override
    protected void mountainSetup() {
        symmetryCliffs = landNoiseMap.copy();
        symmetryCliffs.setVisualName("Symmetry Cliffs: ");
        symmetryCliffs.supcomGradient();
        symmetryCliffs.setToValue(symmetryLines.copy().inflate(3).invert(), 0f);

        mountains.setSize(map.getSize() + 1).set((x, y) -> true);
    }

    @Override
    protected void setupMountainHeightmapPipeline() {
        heightmapMountains.setSize(map.getSize() + 1);
        heightmapMountains.useBrushWithCliffMap(symmetryCliffs, mountainBrushSize);
        heightmapMountains.scaleToNewMinAndMaxHeight(0, noiseScaleMaxToValue - 5f);
        heightmapMountains.set((x, y) -> heightmapMountains.get(x, y) <= 0 ? -128f : heightmapMountains.get(x, y));

        BooleanMask paintedMountains = heightmapMountains.copyAsBooleanMask(plateauHeight / 2);

        mountains.init(paintedMountains);
    }

    @Override
    protected void initRamps() {
        ramps.startVisualDebugger("Ramps: ");
        rampNoise.startVisualDebugger("Ramp Noise: ");
        ramps.setSize(landNoiseMap.getSize());

        for (FlattenParams flattenParams : fractalType.flattenParams) {
            if (flattenParams.hasRamps) {
                BooleanMask layer = landNoiseMap.copyAsBooleanMask(0f, flattenParams.maxHeight);
                layer.startVisualDebugger();
                layer.outline();

                rampNoise.setSize(landNoiseMap.getSize() / 16);
                rampNoise.addWhiteNoise(0, 1);
                rampNoise.setSize(landNoiseMap.getSize());

                layer.subtract(rampNoise.copyAsBooleanMask(0f, 0.8f));

                ramps.add(layer);
            }
        }
    }

    @Override
    protected void blurRamps() {
        BooleanMask inflatedRamps = ramps.copy();
        heightmap.blur(48, inflatedRamps)
                 .blur(32, inflatedRamps.inflate(2))
                 .blur(4, inflatedRamps.inflate(4))
                 .blur(4, inflatedRamps.inflate(8))
                 .clampMin(0f)
                 .clampMax(255f);
    }

    @Override
    protected void setupHeightmapPipeline() {
        int mapSize = map.getSize();

        initRamps();

        landNoiseMap.startVisualDebugger("LandNoiseMap: ");

        heightmap.setSize(mapSize + 1);
        heightmapLand.setSize(mapSize + 1).startVisualDebugger("Heightmap Land: ");
        heightMapNoise.setSize(mapSize / 128);

        // Start the land height as the noise map
        heightmapLand.add(landNoiseMap);

        for (FlattenParams flattenParams : fractalType.flattenParams) {
            MapMaskMethods.flattenHeightBand(heightmapLand, landNoiseMap, flattenParams.minHeight,
                                             flattenParams.maxHeight, flattenParams.destinationMinHeight,
                                             flattenParams.destinationMaxHeight,
                                             flattenParams.slope, flattenParams.blurAmount);
        }


        // Blur and add mountains along the line of symmetry
        if (Set.of(Symmetry.QUAD, Symmetry.DIAG, Symmetry.POINT2, Symmetry.POINT3, Symmetry.POINT4, Symmetry.POINT5,
                   Symmetry.POINT6, Symmetry.POINT7, Symmetry.POINT8, Symmetry.POINT9, Symmetry.POINT10,
                   Symmetry.POINT11, Symmetry.POINT12, Symmetry.POINT13, Symmetry.POINT14, Symmetry.POINT15,
                   Symmetry.POINT16).contains(symmetrySettings.terrainSymmetry())
        ) {
            setupMountainHeightmapPipeline();
            heightmapLand.blur(3, symmetryLines.copy().inflate(10));
            heightmap.add(heightmapLand)
                     .max(heightmapMountains);
            heightmap.blur(1, symmetryLines.copy().inflate(10));
        } else {
            heightmap.add(heightmapLand);
        }
        heightmap.add(waterHeight);

        if (heightMapNoise.getSymmetrySettings().spawnSymmetry().isPerfectSymmetry()) {
            heightMapNoise.addWhiteNoise(plateauHeight / 3).resample(mapSize / 64);
            heightMapNoise.addWhiteNoise(plateauHeight / 3).resample(mapSize + 1);
            heightMapNoise.addWhiteNoise(1)
                          .subtractAvg()
                          .clampMin(0f)
                          .setToValue(land.copy().invert().inflate(16), 0f)
                          .blur(mapSize / 16);
            heightmap.add(heightMapNoise);
        }

        if (symmetrySettings.spawnSymmetry().getNumSymPoints() == 3 || symmetrySettings.spawnSymmetry().getNumSymPoints() >= 5) {
            BooleanMask outerCircle =  new BooleanMask(mapSize + 1, random.nextLong(), symmetrySettings, "outerCircle", pipeline);
            outerCircle.fillCircle(new Vector2(mapSize / 2f, mapSize / 2f), mapSize / 2f, true).startVisualDebugger();
            outerCircle.invert();
            heightmap.setToValue(outerCircle, waterHeight);
            heightmap.blur(5, outerCircle.outline().inflate(5));
        }

        blurRamps();
    }

    @Override
    protected void setupSpawnMaskPipeline() {

        spawnMask.startVisualDebugger("Spawn Mask: ");

        if (fractalType.maxSpawnable > fractalType.minSpawnable) {
            spawnMask.add(landNoiseMap.copyAsBooleanMask(fractalType.minSpawnable, fractalType.maxSpawnable));
        }
        for (FlattenParams flattenParams : fractalType.flattenParams) {
            if (flattenParams.spawnable) {
                spawnMask.add(landNoiseMap.copyAsBooleanMask(flattenParams.minHeight, flattenParams.maxHeight)
                                          .deflate(flattenParams.spawnMaskDeflate));
            }
        }

        spawnMask.subtract(unbuildable)
                 .deflate(4);
    }

    @Override
    protected int getTeamSeparation() {
        if (generatorParameters.numTeams() < 2) {
            return 0;
        } else if (generatorParameters.numTeams() == 2) {
            return map.getSize() / fractalType.teamSeparation;
        } else {
            return StrictMath.min(map.getSize() / generatorParameters.numTeams(), 256);
        }
    }
}
