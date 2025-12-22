package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.FractalFlattenParams;
import com.faforever.neroxis.generator.FractalParams;
import com.faforever.neroxis.generator.FractalWaterMasks;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.util.Pipeline;

import java.util.List;

public class SetonishLastTerrainGenerator extends FractalNoiseLastTerrainGenerator {
    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, Pipeline pipeline) {
        if (map.getSize() < 512) {
            // Small maps are very problematic, because of a lack of spawnable land area, and low mex count
            // This increases the land area of the map, and removes the water
            fractalParams = new FractalParams(
                    16, FractalWaterMasks.NONE, 2, 1.5f, 5, 2, 4,
                    List.of(
                            new FractalFlattenParams(0f, 0.5f, 0, 8, 0.25f, 0, false, false, 4),
                            new FractalFlattenParams(0.5f, 1f, 8, 16, 1f, 0, true, false, 4),
                            new FractalFlattenParams(1f, 27, 18, 18, 0, 1, false, true, 8),
                            new FractalFlattenParams(27, 50, 18, 35, 1, 1, false, false, 4)
                    )
            );
        } else {
            fractalParams = new FractalParams(
                    16, FractalWaterMasks.SETONS, 2, 1.5f, 5, 2, 8,
                    List.of(
                            new FractalFlattenParams(0f, 1.0f, 0, 8, 1f, 0, false, false, 4),
                            new FractalFlattenParams(1.0f, 3f, 8, 16, 1f, 0, true, false, 4),
                            new FractalFlattenParams(3f, 27, 18, 18, 0, 1, false, true, 4),
                            new FractalFlattenParams(27, 50, 18, 35, 1, 1, false, false, 4)
                    )
            );
        }
        super.initialize(map, seed, generatorParameters, symmetrySettings, pipeline);
    }

    @Override
    protected void setupHeightmapPipeline() {
        // This extra step in the heightmap pipeline creates islands in the water area
        // It raises the underwater mountains to be above water
        if (waterMask != FractalWaterMasks.NONE) {
            landNoiseMap.multiply(waterAreaBlur.copy().add(1f).scaleExponentially(1.5f));
        }

        super.setupHeightmapPipeline();
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
    protected int getTeamSeparation() {
        return map.getSize() / 3;
    }
}
