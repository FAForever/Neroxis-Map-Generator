package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.ElementGenerator;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;
import lombok.Getter;

@Getter
public abstract class TerrainGenerator extends ElementGenerator {
    protected FloatMask heightmap;
    protected BooleanMask impassable;
    protected BooleanMask unbuildable;
    protected BooleanMask passable;
    protected BooleanMask passableLand;
    protected BooleanMask passableWater;
    protected FloatMask slope;

    public void setHeightmapImage() {
        Pipeline.await(heightmap);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "setHeightMap", () -> heightmap.getFinalMask()
                                                                                                 .writeToImage(
                                                                                                         map.getHeightmap(),
                                                                                                         1
                                                                                                         /
                                                                                                         map.getHeightMapScale()));
    }

    @Override
    public void setupPipeline() {
        terrainSetup();
        //ensure heightmap is symmetric
        heightmap.forceSymmetry();
        passableSetup();
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        heightmap = new FloatMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "heightmap", true);
        slope = new FloatMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "slope", true);
        impassable = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "impassable", true);
        unbuildable = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "unbuildable", true);
        passable = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passable", true);
        passableLand = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passableLand", true);
        passableWater = new BooleanMask(map.getSize() + 1, random.nextLong(), symmetrySettings, "passableWater", true);
    }

    protected abstract void terrainSetup();

    protected void passableSetup() {
        BooleanMask actualLand = heightmap.copyAsBooleanMask(
                generatorParameters.biome().waterSettings().getElevation());

        slope.init(heightmap.copy().supcomGradient());
        impassable.init(slope, .7f);
        unbuildable.init(slope, .05f);

        impassable.inflate(4);

        passable.init(impassable).invert();
        passableLand.init(actualLand);
        passableWater.init(actualLand).invert();

        passable.fillEdge(8, false);
        passableLand.multiply(passable);
        passableWater.deflate(16).fillEdge(8, false);
    }
}
