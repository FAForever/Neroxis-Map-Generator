package com.faforever.neroxis.map.generator.prop;

import com.faforever.neroxis.map.*;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.placement.UnitPlacer;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.mask.BooleanMask;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.Util;

import java.util.ArrayList;

public strictfp class LargeBattlePropGenerator extends ReducedNaturalPropGenerator {

    protected BooleanMask landWreckMask;
    protected BooleanMask noWrecks;

    public LargeBattlePropGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.5f, 1f)
                .reclaimDensity(.75f, 1f)
                .build();
        weight = 2;
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        landWreckMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "landWreckMask", true);
        noWrecks = new BooleanMask(1, random.nextLong(), symmetrySettings);
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
        landWreckMask.inflate(20f).setSize(mapSize / 4);
        landWreckMask.erode(.5f).setSize(mapSize + 1);
        landWreckMask.multiply(passableLand).fillEdge(96, false);
    }

    protected void generateUnitExclusionMasks() {
        noWrecks.init(passableLand.getFinalMask().invert());
        generateExclusionZones(noWrecks, 128, 4, 32);
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(landWreckMask);
            Util.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
                Army army17 = new Army("ARMY_17", new ArrayList<>());
                Group army17Wreckage = new Group("WRECKAGE", new ArrayList<>());
                army17.addGroup(army17Wreckage);
                map.addArmy(army17);
                BooleanMask placementMask = landWreckMask.getFinalMask().subtract(noWrecks);
                unitPlacer.placeUnits(placementMask, UnitPlacer.T1_Land, army17, army17Wreckage, 2f, 8f);
                unitPlacer.placeUnits(placementMask, UnitPlacer.T2_Land, army17, army17Wreckage, 8f, 12f);
                unitPlacer.placeUnits(placementMask, UnitPlacer.T3_Land, army17, army17Wreckage, 30f, 40f);
            });
        }
    }
}
