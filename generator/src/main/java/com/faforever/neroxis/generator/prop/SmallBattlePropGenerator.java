package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.generator.util.serial.GeneratorParameters;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.UnitPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;

import java.util.random.RandomGenerator;

public class SmallBattlePropGenerator extends ReducedNaturalPropGenerator {
    protected BooleanMask landWreckMask;

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, random, generatorParameters, symmetrySettings, terrainGenerator);
        landWreckMask = new BooleanMask(1, random.split(), symmetrySettings, "landWreckMask");
    }

    @Override
    public void placeUnits() {
        if (generatorParameters.canPlaceUnits()) {
            BooleanMask noWrecks = generateUnitExclusionMasks();
            DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeUnits", () -> {
                Army army17 = new Army("ARMY_17");
                Group army17Wreckage = new Group("WRECKAGE");
                army17.addGroup(army17Wreckage);
                map.addArmy(army17);
                BooleanMask placementMask = landWreckMask.getFinalMask().subtract(noWrecks);
                unitPlacer.placeUnits(placementMask, UnitPlacer.T1_Land, army17, army17Wreckage, 3f, 4f);
                unitPlacer.placeUnits(placementMask, UnitPlacer.T2_Land, army17, army17Wreckage, 5f, 8f);
            });
        }
    }

    @Override
    public void setupPipeline() {
        super.setupPipeline();
        setupWreckPipeline();
    }

    protected void setupWreckPipeline() {
        int mapSize = map.getSize();
        float wreckDensity = reclaimDensity * 0.6f + 0.4f;
        landWreckMask.setSize(mapSize / 8);

        landWreckMask.randomize(wreckDensity * .005f).setSize(mapSize + 1);
        landWreckMask.inflate(6).multiply(passableLand).fillEdge(32, false);
    }

    protected BooleanMask generateUnitExclusionMasks() {
        BooleanMask noWrecks = new BooleanMask(1, random.split(), symmetrySettings, "noWrecks");
        noWrecks.init(unbuildable.getFinalMask());
        generateExclusionZones(noWrecks, 128, 8, 32);
        return noWrecks;
    }
}
