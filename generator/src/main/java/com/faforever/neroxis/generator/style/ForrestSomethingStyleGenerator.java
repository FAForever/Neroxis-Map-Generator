package com.faforever.neroxis.generator.style;

import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.prop.ForrestSomethingPropGenerator;
import com.faforever.neroxis.generator.prop.PropGenerator;
import com.faforever.neroxis.generator.resource.OneMexPerSpawnResourceGenerator;
import com.faforever.neroxis.generator.resource.ResourceGenerator;
import com.faforever.neroxis.generator.terrain.BasicLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.BasicTerrainGenerator;
import com.faforever.neroxis.generator.terrain.BigIslandsTerrainGenerator;
import com.faforever.neroxis.generator.terrain.CenterLakeLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.CenterLakeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.DropPlateauLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.DropPlateauTerrainGenerator;
import com.faforever.neroxis.generator.terrain.FractalLandLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.FractalPlateauLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LandBridgeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.LittleMountainTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MountainRangeLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MountainRangeTerrainGenerator;
import com.faforever.neroxis.generator.terrain.MultiLevelLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.OneIslandTerrainGenerator;
import com.faforever.neroxis.generator.terrain.RiversTerrainGenerator;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyLastTerrainGenerator;
import com.faforever.neroxis.generator.terrain.ValleyTerrainGenerator;

public class ForrestSomethingStyleGenerator extends StyleGenerator {

    @Override
    protected WeightedOptionsWithFallback<PropGenerator> getPropGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new ForrestSomethingPropGenerator());
    }

    @Override
    protected WeightedOptionsWithFallback<ResourceGenerator> getResourceGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new OneMexPerSpawnResourceGenerator());
    }

    protected WeightedOptionsWithFallback<TerrainGenerator> getTerrainGeneratorOptions() {
        return WeightedOptionsWithFallback.of(new BasicLastTerrainGenerator(),
        new WeightedOption<>(new BasicLastTerrainGenerator(), 1f),
        new WeightedOption<>(new BasicTerrainGenerator(), 1f),
        new WeightedOption<>(new BigIslandsTerrainGenerator(), 1f),
        new WeightedOption<>(new CenterLakeLastTerrainGenerator(), 1f),
        new WeightedOption<>(new CenterLakeTerrainGenerator(), 1f),
        new WeightedOption<>(new DropPlateauLastTerrainGenerator(), 1f),
        new WeightedOption<>(new DropPlateauTerrainGenerator(), 1f),
        new WeightedOption<>(new FractalLandLastTerrainGenerator(), 1f),
        new WeightedOption<>(new FractalPlateauLastTerrainGenerator(), 1f),
        new WeightedOption<>(new LandBridgeTerrainGenerator(), 1f),
        new WeightedOption<>(new LittleMountainLastTerrainGenerator(), 1f),
        new WeightedOption<>(new LittleMountainTerrainGenerator(), 1f),
        new WeightedOption<>(new MountainRangeLastTerrainGenerator(), 1f),
        new WeightedOption<>(new MountainRangeTerrainGenerator(), 1f),
        new WeightedOption<>(new MultiLevelLastTerrainGenerator(), 1f),
        new WeightedOption<>(new OneIslandTerrainGenerator(), 1f),
        new WeightedOption<>(new RiversTerrainGenerator(), 1f),
        new WeightedOption<>(new ValleyLastTerrainGenerator(), 1f),
        new WeightedOption<>(new ValleyTerrainGenerator(), 1f)
        );
    }
}
