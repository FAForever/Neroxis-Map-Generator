package neroxis.generator.prop;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.UnitPlacer;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

import java.util.ArrayList;

public class SmallBattlePropGenerator extends DefaultPropGenerator {

    protected ConcurrentBinaryMask t1LandWreckMask;
    protected ConcurrentBinaryMask t2LandWreckMask;
    protected BinaryMask noProps;
    protected BinaryMask noWrecks;

    public SmallBattlePropGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.25f, 1f)
                .reclaimDensity(.25f, 1f)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        t1LandWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "t1LandWreckMask");
        t2LandWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "t2LandWreckMask");
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
        t1LandWreckMask.setSize(mapSize / 8);
        t2LandWreckMask.setSize(mapSize / 8);

        t1LandWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .0025f).setSize(mapSize + 1);
        t1LandWreckMask.intersect(passableLand).fillEdge(20, false);
        t2LandWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .002f).setSize(mapSize + 1);
        t2LandWreckMask.intersect(passableLand).minus(t1LandWreckMask).fillEdge(64, false);
    }

    protected void generateUnitExclusionMasks() {
        noWrecks.init(unbuildable.getFinalMask());
        generateExclusionZones(noWrecks, 128, 8, 32);
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(t1LandWreckMask, t2LandWreckMask);
            long sTime = System.currentTimeMillis();
            Army army17 = new Army("ARMY_17", new ArrayList<>());
            Group army17Initial = new Group("INITIAL", new ArrayList<>());
            Group army17Wreckage = new Group("WRECKAGE", new ArrayList<>());
            army17.addGroup(army17Initial);
            army17.addGroup(army17Wreckage);
            map.addArmy(army17);
            unitPlacer.placeUnits(t1LandWreckMask.getFinalMask().minus(noWrecks), UnitPlacer.T1_Land, army17, army17Wreckage, 1f, 4f);
            unitPlacer.placeUnits(t2LandWreckMask.getFinalMask().minus(noWrecks), UnitPlacer.T2_Land, army17, army17Wreckage, 30f);
            if (DEBUG) {
                System.out.printf("Done: %4d ms, %s, placeUnits\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInClass(neroxis.generator.style.DefaultStyleGenerator.class));
            }
        }
    }
}
