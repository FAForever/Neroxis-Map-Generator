package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.Visibility;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.UnitPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public class NavyWrecksPropGenerator extends ReducedNaturalPropGenerator {
    protected BooleanMask t2NavyWreckMask;
    protected BooleanMask navyFactoryWreckMask;
    protected BooleanMask noWrecks;

    @Override
    public ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder().reclaimDensity(.5f, 1f).landDensity(0f, .5f).build();
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, symmetrySettings, terrainGenerator);
        t2NavyWreckMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "t2NavyWreckMask", true);
        navyFactoryWreckMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "navyFactoryWreckMask", true);
        noWrecks = new BooleanMask(1, random.nextLong(), symmetrySettings);
    }

    @Override
    public void placeUnits() {
        if ((generatorParameters.visibility() != Visibility.UNEXPLORED)) {
            generateUnitExclusionMasks();
            Pipeline.await(t2NavyWreckMask, navyFactoryWreckMask);
            DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
                Army army17 = new Army("ARMY_17");
                Group army17Wreckage = new Group("WRECKAGE");
                army17.addGroup(army17Wreckage);
                map.addArmy(army17);
                unitPlacer.placeUnits(t2NavyWreckMask.getFinalMask().subtract(noWrecks), UnitPlacer.T2_Navy, army17,
                                      army17Wreckage, 128f);
                unitPlacer.placeUnits(navyFactoryWreckMask.getFinalMask().subtract(noWrecks), UnitPlacer.Navy_Factory,
                                      army17, army17Wreckage, 256f);
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
        float reclaimDensity = generatorParameters.reclaimDensity();
        t2NavyWreckMask.setSize(mapSize + 1);
        navyFactoryWreckMask.setSize(mapSize + 1);

        navyFactoryWreckMask.add(passableLand.copy().inflate(48))
                            .subtract(passableLand.copy().inflate(16))
                            .fillEdge(20, false)
                            .fillCenter(32, false);
        navyFactoryWreckMask.flipValues((reclaimDensity * .8f + random.nextFloat() * .2f) * .001f).inflate(8);
        t2NavyWreckMask.add(passableLand.copy().inflate(8).outline()).fillEdge(20, false);
        t2NavyWreckMask.flipValues((reclaimDensity * .8f + random.nextFloat() * .2f) * .001f).inflate(8);
    }

    protected void generateUnitExclusionMasks() {
        noWrecks.init(passableLand.getFinalMask()).add(impassable.getFinalMask());
        generateExclusionZones(noWrecks, 64, 8, 32);
    }
}
