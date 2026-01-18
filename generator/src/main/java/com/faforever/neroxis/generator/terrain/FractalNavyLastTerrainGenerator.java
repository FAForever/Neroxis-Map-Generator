package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.FractalWaterMasks;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.vector.Vector2;

import java.util.List;
import java.util.Random;

public class FractalNavyLastTerrainGenerator extends FractalNoiseLastTerrainGenerator {

    private  FractalWaterMasks randomWaterMask;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {

        randomWaterMask = WeightedOptionsWithFallback.of(
                FractalWaterMasks.NONE,
                new WeightedOption<>(FractalWaterMasks.SYMMETRY_LINE, 1f),
                new WeightedOption<>(FractalWaterMasks.HOUR_GLASS, 1f),
                new WeightedOption<>(FractalWaterMasks.CENTER_LAKE, 1f)
        ).select(new Random(seed));

        if (map.getSize() < 512) {
            // Small maps are very problematic, because of a lack of spawnable land area, and low mex count
            // This increases the area of the map dedicated to spawnable land and mexes
            fractalParams = new FractalParams(
                    16, randomWaterMask, 2, 1.5f, 5, 2, 4, 50,
                    List.of(
                            new FractalFlattenParams(0f, 0.5f, 0, 8, 0.25f, 0, false, false, 4),
                            new FractalFlattenParams(0.5f, 1f, 8, 16, 1f, 0, true, false, 4),
                            new FractalFlattenParams(1f, 27, 18, 18, 0, 1, false, true, 8),
                            new FractalFlattenParams(27, 50, 18, 35, 1, 1, false, false, 4)
                            )
            );
        } else {
            // This is a fractal navy map, works well for 10K - 20K maps, with a good amount of the map being ocean.
            fractalParams = new FractalParams(
                    16, randomWaterMask, 2, 1.5f, 5, 2, 4, 20,
                    List.of(
                            new FractalFlattenParams(0.0f, 3f, 6, 16, 3f, 0, true, false, 4),
                            new FractalFlattenParams(3f, 27, 18, 18, 0, 1, false, true, 4),
                            new FractalFlattenParams(27, 50, 18, 35, 1, 1, false, false, 4)
                            )
            );
        }

        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
    }

    @Override
    protected void addWaterAreasToNoiseMap(int mapSize) {
        if (randomWaterMask != FractalWaterMasks.NONE) {
            switch (waterMask) {
                case FractalWaterMasks.SYMMETRY_LINE -> {
                    // Water will be more likely along the symmetry line(s), kinda splitting the map in half, or pie slices for odd symmetries
                    waterArea.drawSymmetryLines(symmetrySettings.terrainSymmetry());
                    waterArea.inflate(
                            StrictMath.min(256, mapSize / 4f / symmetrySettings.teamSymmetry().getNumSymPoints()));
                    waterAreaBlur = waterArea.copyAsFloatMask(0f, 1f);
                    waterAreaBlur.blur(mapSize / 3 / symmetrySettings.teamSymmetry().getNumSymPoints());
                }
                case FractalWaterMasks.HOUR_GLASS -> {
                    // An unusual shape, which increase the likelihood of water along the symmetry lines and the corners of the map
                    waterArea.drawSymmetryLines(symmetrySettings.terrainSymmetry());
                    waterArea.inflate(mapSize / 4f / symmetrySettings.teamSymmetry().getNumSymPoints());
                    List<Vector2> symmetryPoints = waterArea.getSymmetryPointsWithOutOfBounds(new Vector2(0, 0),
                                                                                              SymmetryType.SPAWN)
                                                            .stream()
                                                            .map(Vector2::roundToNearestHalfPoint)
                                                            .toList();
                    waterArea.fillCircle(new Vector2(0, 0),
                                         mapSize / 2f / symmetrySettings.teamSymmetry().getNumSymPoints(), true);
                    symmetryPoints.forEach(
                            s -> waterArea.fillCircle(s,
                                                      mapSize / 2f / symmetrySettings.teamSymmetry().getNumSymPoints(),
                                                      true));
                    waterAreaBlur = waterArea.copyAsFloatMask(0f, 1f);
                    waterAreaBlur.blur(mapSize / 3 / symmetrySettings.teamSymmetry().getNumSymPoints());
                }
                case FractalWaterMasks.CENTER_LAKE -> {
                    // big ocean in the centre of the map
                    waterArea.fillCircle(new Vector2(mapSize / 2f, mapSize / 2f), mapSize / 3f, true);
                    waterAreaBlur = waterArea.copyAsFloatMask(0f, 1f);
                    waterAreaBlur.blur(mapSize / 8);
                }
            }
            landNoiseMap.add(1f);
            landNoiseMap.subtract(waterAreaBlur);
        }
    }
}
