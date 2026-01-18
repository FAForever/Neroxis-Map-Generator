package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.Visibility;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.UnitPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;

public class LargeBattlePropGenerator extends ReducedNaturalPropGenerator {
    protected BooleanMask landWreckMask;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        landWreckMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "landWreckMask");
    }

    @Override
    public void placeUnits() {
        if ((generatorParameters.visibility() != Visibility.UNEXPLORED)) {
            BooleanMask noWrecks = generatePropExclusionMasks();
            DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
                Army army17 = new Army("ARMY_17");
                Group army17Wreckage = new Group("WRECKAGE");
                army17.addGroup(army17Wreckage);
                map.addArmy(army17);
                BooleanMask placementMask = landWreckMask.getFinalMask().subtract(noWrecks);
                unitPlacer.placeUnits(placementMask, UnitPlacer.T1_Land, army17, army17Wreckage, 2f, 8f);
                unitPlacer.placeUnits(placementMask, UnitPlacer.T2_Land, army17, army17Wreckage, 8f, 12f);
                unitPlacer.placeUnits(placementMask, UnitPlacer.T3_Land, army17, army17Wreckage, 30f, 40f);
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

        landWreckMask.setSize(mapSize + 1);
        landWreckMask.fillCenter(196, true);
        map.getSpawns().forEach(spawn -> landWreckMask.fillCircle(spawn.getPosition(), 128, false));
        landWreckMask.multiply(passableLand).space(96, 128);
        landWreckMask.inflate(20).setSize(mapSize / 4);
        landWreckMask.erode(.5f).setSize(mapSize + 1);
        landWreckMask.multiply(passableLand).fillEdge(96, false);
    }

    protected BooleanMask generateUnitExclusionMasks() {
        BooleanMask noWrecks = new BooleanMask(1, random.nextLong(), symmetrySettings, "noWrecks");
        noWrecks.init(passableLand.getFinalMask().invert());
        generateExclusionZones(noWrecks, 128, 4, 32);
        return noWrecks;
    }
}
