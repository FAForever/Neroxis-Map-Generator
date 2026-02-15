package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.FractalWaterMasks;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;
import java.util.Random;

public class FractalNavyLastTerrainGenerator extends FractalNoiseLastTerrainGenerator {

    private FractalWaterMasks randomWaterMask;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        randomWaterMask = WeightedOptionsWithFallback.of(
                FractalWaterMasks.NONE,
                new WeightedOption<>(FractalWaterMasks.NONE, 1f),
                new WeightedOption<>(FractalWaterMasks.SYMMETRY_LINE, 1f),
                new WeightedOption<>(FractalWaterMasks.HOUR_GLASS, 1f),
                new WeightedOption<>(FractalWaterMasks.LAKE_AROUND_ISLAND, 1f)
        ).select(new Random(seed));
        if (map.getSize() < 512) {
            // Small maps are very problematic, because of a lack of spawnable land area, and low mex count
            // This increases the area of the map dedicated to spawnable land and mexes
            fractalParams = new FractalParams(
                    15, randomWaterMask, 2, 2, 1.5f, 5, 2, 4, 50,
                    List.of(
                            new FractalFlattenParams(0f, 0.5f, 0, 8, 0.25f, 0, false, 0f, false, 4),
                            new FractalFlattenParams(0.5f, 1f, 8, 16, 1f, 0, true, 0f, false, 4),
                            new FractalFlattenParams(1f, 27, 18, 18, 0, 1, false, 0.1f, true, 8),
                            new FractalFlattenParams(27, 50, 18, 35, 1, 1, false, 0f, false, 4)
                    )
            );
        } else {
            // This is a fractal navy map, works well for 10K - 20K maps, with a good amount of the map being ocean.
            fractalParams = new FractalParams(
                    16, randomWaterMask, 2, 2, 1.5f, 5, 2, 4, 20,
                    List.of(
                            new FractalFlattenParams(0.0f, 3.0f, 6, 6, 1f, 2, true, 0.15f, false, 4),
                            new FractalFlattenParams(3.0f, 27, 17, 18, 1, 0, false, 0f, true, 4),
                            new FractalFlattenParams(27, 50, 18, 35, 0.5f, 0, false, 0f, false, 4)
                    )
            );
        }

        super.initialize(map, seed, generatorParameters, symmetrySettings);
    }

    @Override
    protected void setMaxNoiseOctaves() {
        if (map.getSize() > 768) {
            maxNoiseOctaves = 8;
        } else {
            maxNoiseOctaves = 7;
        }
    }

    @Override
    protected void addWaterAreasToNoiseMap(int mapSize) {
        float waterStength = 1f;
        int waterBlurRadius = mapSize / 8;

        if (randomWaterMask != FractalWaterMasks.NONE) {
            switch (waterMask) {
                case FractalWaterMasks.SYMMETRY_LINE -> {
                    // Water will be more likely along the symmetry line(s), kinda splitting the map in half, or pie slices for odd symmetries
                    waterArea.drawSymmetryLines(symmetrySettings.terrainSymmetry());
                    waterArea.inflate(
                            (int) StrictMath.min(256,
                                                 mapSize / 5f / symmetrySettings.teamSymmetry().getNumSymPoints()));
                }
                case FractalWaterMasks.HOUR_GLASS -> {
                    // Big ocean in the centre of the map
                    waterArea.fillCircle(new Vector2(mapSize / 2f, mapSize / 2f), mapSize / 3f, true);

                    // Make some central island areas
                    int islandX = mapSize / 3;
                    int islandY = mapSize / 3;
                    int randSize = random.nextInt(mapSize / 6, mapSize / 4);

                    List<Vector2> symmetryPoints = waterArea.getSymmetryPointsWithOutOfBounds(
                                                                    new Vector2(islandX, islandY),
                                                                    SymmetryType.SPAWN)
                                                            .stream()
                                                            .map(Vector2::roundToNearestHalfPoint)
                                                            .toList();

                    waterArea.fillCircle(new Vector2(islandX, islandY), randSize, false);
                    symmetryPoints.forEach(
                            s -> waterArea.fillCircle(s, randSize, false)
                    );

                    waterStength = 2.0f;
                }
                case FractalWaterMasks.LAKE_AROUND_ISLAND -> {
                    // Big ocean in the centre of the map
                    waterArea.fillCircle(new Vector2(mapSize / 2f, mapSize / 2f), mapSize / 3f, true);

                    // Centre Island
                    waterArea.fillCircle(new Vector2(mapSize / 2f, mapSize / 2f), mapSize / 4.5f, false);

                    waterStength = 2.2f;
                    waterBlurRadius = mapSize / 16;
                }

            }

            waterAreaBlur = waterArea.copyAsFloatMask(0f, waterStength);
            waterAreaBlur.blur(waterBlurRadius);
            landNoiseMap.add(1f);
            landNoiseMap.subtract(waterAreaBlur);
        }
    }

    @Override
    protected void setupMountainHeightmapPipeline() {
        // Draw some mountains specially implemented for Setons
        rawMountains.setSize(map.getSize() + 1);
        String brushName = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));

        float densityMultiplier = 1f / (1024f / map.getSize());

        // Mountains within the main land area
        rawMountains.useBrushWithinAreaWithDensity(
                landNoiseMap.copyAsBooleanMask(fractalParams.fractalFlattenParams().get(2).minHeight(),
                                               fractalParams.fractalFlattenParams().get(2).maxHeight()).deflate(5)
                , brushName, 25, densityMultiplier, 0.75f, false);
    }

    @Override
    protected void blurRamps() {
        BooleanMask inflatedRamps = ramps.copy();

        heightmap.blur(4, inflatedRamps.copy().inflate(4))
                 .blur(4, inflatedRamps.copy().inflate(4).outline().inflate(2))
                 .blur(2, inflatedRamps.copy().inflate(4).outline().inflate(4))

                 .blur(8, inflatedRamps.copy().inflate(8))
                 .blur(2, inflatedRamps.copy().inflate(8).outline().inflate(4))

                 .blur(16, inflatedRamps.copy().inflate(16))
                 .blur(2, inflatedRamps.copy().inflate(16).outline().inflate(4))

                 .blur(4, inflatedRamps.copy().inflate(32))
                 .blur(4, inflatedRamps.copy().inflate(32))
                 .blur(2, inflatedRamps.copy().inflate(32).outline().inflate(4))

                 .clampMin(0f)
                 .clampMax(255f);
    }

    @Override
    protected int getTeammateSeparation() {
        // This spaces teammates as far as possible from each other.
        // On a 20k 4v4 teammates will be 128 apart, making for a better Setons game
        int numTeams = generatorParameters.numTeams();
        int spawnsPerTeam = numTeams > 0 ? generatorParameters.spawnCount() / numTeams : 1;
        if (spawnsPerTeam <= 0) {
            spawnsPerTeam = 1;
        }
        return map.getSize() / 6 / spawnsPerTeam * 4;
    }

    @Override
    public int getTeamSeparation() {
        return map.getSize() / 3;
    }

}
