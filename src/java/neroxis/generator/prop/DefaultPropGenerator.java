package neroxis.generator.prop;

import neroxis.biomes.Biome;
import neroxis.generator.MapGenerator;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

public class DefaultPropGenerator extends PropGenerator {

    protected ConcurrentBinaryMask treeMask;
    protected ConcurrentBinaryMask cliffRockMask;
    protected ConcurrentBinaryMask fieldStoneMask;
    protected BinaryMask noProps;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        treeMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "treeMask");
        cliffRockMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "cliffRockMask");
        fieldStoneMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "fieldStoneMask");
        noProps = new BinaryMask(1, random.nextLong(), symmetrySettings);
    }

    @Override
    public void setupPipeline() {
        setupPropPipeline();
    }

    protected void setupPropPipeline() {
        int mapSize = map.getSize();
        float reclaimDensity = mapParameters.getReclaimDensity();
        treeMask.setSize(mapSize / 16);
        cliffRockMask.setSize(mapSize / 16);
        fieldStoneMask.setSize(mapSize / 4);

        cliffRockMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .5f + .1f).setSize(mapSize + 1);
        cliffRockMask.intersect(impassable).grow(.5f, SymmetryType.SPAWN, 6).minus(impassable).intersect(passableLand);
        fieldStoneMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .001f).setSize(mapSize + 1);
        fieldStoneMask.intersect(passableLand).fillEdge(10, false);
        treeMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .2f + .1f).setSize(mapSize / 4);
        treeMask.inflate(2).erode(.5f, SymmetryType.SPAWN).erode(.5f, SymmetryType.SPAWN);
        treeMask.setSize(mapSize + 1);
        treeMask.intersect(passableLand.copy().deflate(8)).fillEdge(8, false);
    }


    protected void generatePropExclusionMasks() {
        noProps.init(unbuildable.getFinalMask());

        generateExclusionZones(noProps, 30, 1, 8);
    }

    protected void generateExclusionZones(BinaryMask mask, float spawnSpacing, float mexSpacing, float hydroSpacing) {
        map.getSpawns().forEach(spawn -> mask.fillCircle(spawn.getPosition(), spawnSpacing, true));
        map.getMexes().forEach(mex -> mask.fillCircle(mex.getPosition(), mexSpacing, true));
        map.getHydros().forEach(hydro -> mask.fillCircle(hydro.getPosition(), hydroSpacing, true));
    }

    @Override
    public void placeProps() {
        long sTime = System.currentTimeMillis();
        generatePropExclusionMasks();
        if (MapGenerator.DEBUG) {
            System.out.printf("Done: %4d ms, %s, exclusionMasks\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInPackage("neroxis.generator"));
        }
        placePropsWithExclusion();
    }

    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, cliffRockMask, fieldStoneMask);
        long sTime = System.currentTimeMillis();
        Biome biome = mapParameters.getBiome();
        propPlacer.placeProps(treeMask.getFinalMask().minus(noProps), biome.getPropMaterials().getTreeGroups(), 3f, 7f);
        propPlacer.placeProps(cliffRockMask.getFinalMask().minus(noProps), biome.getPropMaterials().getRocks(), .5f, 2f);
        propPlacer.placeProps(fieldStoneMask.getFinalMask().minus(noProps), biome.getPropMaterials().getBoulders(), 30f);
        if (MapGenerator.DEBUG) {
            System.out.printf("Done: %4d ms, %s, placeProps\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInPackage("neroxis.generator"));
        }
    }

    @Override
    public void placeUnits() {
    }
}
