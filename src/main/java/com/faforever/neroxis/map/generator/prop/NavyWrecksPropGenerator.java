package com.faforever.neroxis.map.generator.prop;

import com.faforever.neroxis.map.*;
import com.faforever.neroxis.map.generator.ParameterConstraints;
import com.faforever.neroxis.map.generator.placement.UnitPlacer;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.mask.BooleanMask;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.Util;

import java.util.ArrayList;

public strictfp class NavyWrecksPropGenerator extends ReducedNaturalPropGenerator {

    protected BooleanMask t2NavyWreckMask;
    protected BooleanMask navyFactoryWreckMask;
    protected BooleanMask noWrecks;

    public NavyWrecksPropGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .reclaimDensity(.5f, 1f)
                .landDensity(0f, .5f)
                .build();
        weight = 2;
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        t2NavyWreckMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "t2NavyWreckMask", true);
        navyFactoryWreckMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "navyFactoryWreckMask", true);
        noWrecks = new BooleanMask(1, random.nextLong(), symmetrySettings);
    }

    @Override
    public void setupPipeline() {
        super.setupPipeline();
        setupWreckPipeline();
    }

    protected void setupWreckPipeline() {
        int mapSize = map.getSize();
        float reclaimDensity = mapParameters.getReclaimDensity();
        t2NavyWreckMask.setSize(mapSize + 1);
        navyFactoryWreckMask.setSize(mapSize + 1);

        navyFactoryWreckMask.add(passableLand.copy().inflate(48)).subtract(passableLand.copy().inflate(16)).fillEdge(20, false).fillCenter(32, false);
        navyFactoryWreckMask.flipValues((reclaimDensity * .8f + random.nextFloat() * .2f) * .001f).inflate(8);
        t2NavyWreckMask.add(passableLand.copy().inflate(8).outline()).fillEdge(20, false);
        t2NavyWreckMask.flipValues((reclaimDensity * .8f + random.nextFloat() * .2f) * .001f).inflate(8);
    }

    protected void generateUnitExclusionMasks() {
        noWrecks.init(passableLand.getFinalMask()).add(impassable.getFinalMask());
        generateExclusionZones(noWrecks, 64, 8, 32);
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(t2NavyWreckMask, navyFactoryWreckMask);
            Util.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
                Army army17 = new Army("ARMY_17", new ArrayList<>());
                Group army17Wreckage = new Group("WRECKAGE", new ArrayList<>());
                army17.addGroup(army17Wreckage);
                map.addArmy(army17);
                unitPlacer.placeUnits(t2NavyWreckMask.getFinalMask().subtract(noWrecks), UnitPlacer.T2_Navy, army17, army17Wreckage, 128f);
                unitPlacer.placeUnits(navyFactoryWreckMask.getFinalMask().subtract(noWrecks), UnitPlacer.Navy_Factory, army17, army17Wreckage, 256f);
            });
        }
    }
}
