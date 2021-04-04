package neroxis.generator.prop;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.UnitPlacer;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

import java.util.ArrayList;

public class NavyWrecksPropGenerator extends DefaultPropGenerator {

    protected ConcurrentBinaryMask t2NavyWreckMask;
    protected ConcurrentBinaryMask navyFactoryWreckMask;
    protected ConcurrentBinaryMask allWreckMask;
    protected BinaryMask noProps;
    protected BinaryMask noWrecks;

    public NavyWrecksPropGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(0f, .5f)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        t2NavyWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "t2NavyWreckMask");
        navyFactoryWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "navyFactoryWreckMask");
        allWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "allWreckMask");
        noProps = new BinaryMask(1, random.nextLong(), symmetrySettings);
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
        t2NavyWreckMask.setSize(mapSize / 8);
        navyFactoryWreckMask.setSize(mapSize / 8);
        allWreckMask.setSize(mapSize + 1);

        navyFactoryWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .005f).setSize(mapSize + 1);
        navyFactoryWreckMask.intersect(passableLand.copy().inflate(48)).minus(passableLand.copy().inflate(16)).fillEdge(20, false).fillCenter(32, false);
        t2NavyWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .005f).setSize(mapSize + 1);
        t2NavyWreckMask.intersect(passableLand.copy().inflate(4).outline()).fillEdge(20, false);
        allWreckMask.combine(t2NavyWreckMask).inflate(2);
    }

    protected void generateUnitExclusionMasks() {
        noWrecks.init(unbuildable.getFinalMask());
        generateExclusionZones(noWrecks, 128, 8, 32);
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(t2NavyWreckMask, navyFactoryWreckMask);
            long sTime = System.currentTimeMillis();
            Army army17 = new Army("ARMY_17", new ArrayList<>());
            Group army17Initial = new Group("INITIAL", new ArrayList<>());
            Group army17Wreckage = new Group("WRECKAGE", new ArrayList<>());
            army17.addGroup(army17Initial);
            army17.addGroup(army17Wreckage);
            map.addArmy(army17);
            unitPlacer.placeUnits(t2NavyWreckMask.getFinalMask().minus(noWrecks), UnitPlacer.T2_Navy, army17, army17Wreckage, 128f);
            unitPlacer.placeUnits(navyFactoryWreckMask.getFinalMask().minus(noWrecks), UnitPlacer.Navy_Factory, army17, army17Wreckage, 256f);
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, placeUnits\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(neroxis.generator.style.DefaultStyleGenerator.class));
            }
        }
    }
}
