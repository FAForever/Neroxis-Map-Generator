package neroxis.generator.prop;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.placement.UnitPlacer;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

import java.util.ArrayList;

public class NavyWrecksPropGenerator extends ReducedNaturalPropGenerator {

    protected ConcurrentBinaryMask t2NavyWreckMask;
    protected ConcurrentBinaryMask navyFactoryWreckMask;
    protected BinaryMask noWrecks;

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
        t2NavyWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "t2NavyWreckMask");
        navyFactoryWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "navyFactoryWreckMask");
        noWrecks = new BinaryMask(1, random.nextLong(), symmetrySettings);
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

        navyFactoryWreckMask.combine(passableLand.copy().inflate(48)).minus(passableLand.copy().inflate(16)).fillEdge(20, false).fillCenter(32, false);
        navyFactoryWreckMask.flipValues((reclaimDensity * .8f + random.nextFloat() * .2f) * .001f).inflate(8);
        t2NavyWreckMask.combine(passableLand.copy().inflate(8).outline()).fillEdge(20, false);
        t2NavyWreckMask.flipValues((reclaimDensity * .8f + random.nextFloat() * .2f) * .001f).inflate(8);
    }

    protected void generateUnitExclusionMasks() {
        noWrecks.init(passableLand.getFinalMask()).combine(impassable.getFinalMask());
        generateExclusionZones(noWrecks, 64, 8, 32);
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(t2NavyWreckMask, navyFactoryWreckMask);
            Util.timedRun("neroxis.generator", "placeProps", () -> {
                Army army17 = new Army("ARMY_17", new ArrayList<>());
                Group army17Wreckage = new Group("WRECKAGE", new ArrayList<>());
                army17.addGroup(army17Wreckage);
                map.addArmy(army17);
                unitPlacer.placeUnits(t2NavyWreckMask.getFinalMask().minus(noWrecks), UnitPlacer.T2_Navy, army17, army17Wreckage, 128f);
                unitPlacer.placeUnits(navyFactoryWreckMask.getFinalMask().minus(noWrecks), UnitPlacer.Navy_Factory, army17, army17Wreckage, 256f);
            });
        }
    }
}
