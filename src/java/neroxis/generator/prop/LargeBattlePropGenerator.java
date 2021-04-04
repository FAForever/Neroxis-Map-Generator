package neroxis.generator.prop;

import neroxis.biomes.Biome;
import neroxis.generator.MapGenerator;
import neroxis.generator.ParameterConstraints;
import neroxis.generator.UnitPlacer;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

import java.util.ArrayList;

public class LargeBattlePropGenerator extends DefaultPropGenerator {

    protected ConcurrentBinaryMask t1LandWreckMask;
    protected ConcurrentBinaryMask t2LandWreckMask;
    protected ConcurrentBinaryMask t3LandWreckMask;
    protected BinaryMask noWrecks;

    public LargeBattlePropGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.5f, 1f)
                .reclaimDensity(.25f, 1f)
                .build();
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        t1LandWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "t1LandWreckMask");
        t2LandWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "t2LandWreckMask");
        t3LandWreckMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "t3LandWreckMask");
        treeMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "treeMask");
        cliffRockMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "cliffRockMask");
        fieldStoneMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "fieldStoneMask");
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
        t3LandWreckMask.setSize(mapSize / 8);

        t1LandWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .0025f).setSize(mapSize + 1);
        t1LandWreckMask.intersect(passableLand).fillEdge(20, false);
        t2LandWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .002f).setSize(mapSize + 1);
        t2LandWreckMask.intersect(passableLand).minus(t1LandWreckMask).fillEdge(64, false);
        t3LandWreckMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .0004f).setSize(mapSize + 1);
        t3LandWreckMask.intersect(passableLand).minus(t1LandWreckMask).minus(t2LandWreckMask).fillEdge(mapSize / 8, false);
    }

    protected void generateUnitExclusionMasks() {
        noWrecks.init(unbuildable.getFinalMask());
        generateExclusionZones(noWrecks, 128, 8, 32);
    }

    @Override
    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, cliffRockMask, fieldStoneMask);
        long sTime = System.currentTimeMillis();
        Biome biome = mapParameters.getBiome();
        generatePropExclusionMasks();
        propPlacer.placeProps(treeMask.getFinalMask().minus(noProps), biome.getPropMaterials().getTreeGroups(), 3f, 7f);
        propPlacer.placeProps(cliffRockMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), .5f, 3f);
        propPlacer.placeProps(fieldStoneMask.getFinalMask().minus(noProps), biome.getPropMaterials().getBoulders(), 30f);
        if (MapGenerator.DEBUG) {
            System.out.printf("Done: %4d ms, %s, placeProps\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInPackage("neroxis.generator"));
        }
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(t1LandWreckMask, t2LandWreckMask, t3LandWreckMask);
            long sTime = System.currentTimeMillis();
            Army army17 = new Army("ARMY_17", new ArrayList<>());
            Group army17Initial = new Group("INITIAL", new ArrayList<>());
            Group army17Wreckage = new Group("WRECKAGE", new ArrayList<>());
            army17.addGroup(army17Initial);
            army17.addGroup(army17Wreckage);
            map.addArmy(army17);
            unitPlacer.placeUnits(t1LandWreckMask.getFinalMask().minus(noWrecks), UnitPlacer.T1_Land, army17, army17Wreckage, 1f, 4f);
            unitPlacer.placeUnits(t2LandWreckMask.getFinalMask().minus(noWrecks), UnitPlacer.T2_Land, army17, army17Wreckage, 30f);
            unitPlacer.placeUnits(t3LandWreckMask.getFinalMask().minus(noWrecks), UnitPlacer.T3_Land, army17, army17Wreckage, 192f);
            if (MapGenerator.DEBUG) {
                System.out.printf("Done: %4d ms, %s, placeUnits\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInPackage("neroxis.generator"));
            }
        }
    }
}
