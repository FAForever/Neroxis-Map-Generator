package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.DebugUtil;
import lombok.Getter;

import java.util.random.RandomGenerator;

@Getter
public abstract class TerrainGenerator implements HasParameterConstraints {
    protected SCMap map;
    protected RandomGenerator.SplittableGenerator random;
    protected GeneratorParameters generatorParameters;
    protected SymmetrySettings symmetrySettings;

    protected FloatMask heightmap;
    protected BooleanMask impassable;
    protected BooleanMask unbuildable;
    protected BooleanMask passable;
    protected BooleanMask passableLand;
    protected BooleanMask passableWater;
    protected BooleanMask mexDeadZone;
    protected FloatMask slope;

    public abstract void setupPipeline();

    public abstract void placeSpawns();

    public void setHeightmapImage() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "setHeightMap", () -> heightmap.getFinalMask()
                                                                                                 .writeToImage(
                                                                                                         map.getHeightmap(),
                                                                                                         1
                                                                                                         /
                                                                                                         map.getHeightMapScale()));
    }

    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        this.map = map;
        this.random = random.split();
        this.generatorParameters = generatorParameters;
        this.symmetrySettings = symmetrySettings;
        heightmap = new FloatMask(map.getSize() + 1, random.split(), symmetrySettings, "heightmap");
        slope = new FloatMask(map.getSize() + 1, random.split(), symmetrySettings, "slope");
        impassable = new BooleanMask(map.getSize() + 1, random.split(), symmetrySettings, "impassable");
        unbuildable = new BooleanMask(map.getSize() + 1, random.split(), symmetrySettings, "unbuildable");
        passable = new BooleanMask(map.getSize() + 1, random.split(), symmetrySettings, "passable");
        passableLand = new BooleanMask(map.getSize() + 1, random.split(), symmetrySettings, "passableLand");
        passableWater = new BooleanMask(map.getSize() + 1, random.split(), symmetrySettings, "passableWater");
        mexDeadZone = new BooleanMask(map.getSize() + 1, random.split(), symmetrySettings, "mexDeadZone");
    }

    public float getSpawnSeparation() {
        if (generatorParameters.numTeams() < 2) {
            return (float) generatorParameters.mapSize() / generatorParameters.spawnCount() * 1.5f;
        } else if (generatorParameters.numTeams() == 2) {
            return random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16f;
        } else {
            if (generatorParameters.numTeams() < 8) {
                return random.nextInt(map.getSize() / 2 / generatorParameters.numTeams() - map.getSize() / 16) +
                       map.getSize() / 16f;
            } else {
                return 0;
            }
        }
    }

    protected int getTeamSeparation() {
        if (generatorParameters.numTeams() < 2) {
            return 0;
        } else if (generatorParameters.numTeams() == 2) {
            return map.getSize() / 2;
        } else {
            return StrictMath.min(map.getSize() / generatorParameters.numTeams(), 256);
        }
    }

    protected void setupPassablePipeline() {
        BooleanMask actualLand = heightmap.copyAsBooleanMask(
                map.getBiome().waterSettings().elevation());

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
