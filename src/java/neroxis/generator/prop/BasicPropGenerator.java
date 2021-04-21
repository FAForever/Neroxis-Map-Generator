package neroxis.generator.prop;

import neroxis.biomes.Biome;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

public class BasicPropGenerator extends PropGenerator {

    protected BinaryMask treeMask;
    protected BinaryMask cliffRockMask;
    protected BinaryMask fieldStoneMask;
    protected BinaryMask noProps;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        treeMask = new BinaryMask(1, random.nextLong(), symmetrySettings, "treeMask", true);
        cliffRockMask = new BinaryMask(1, random.nextLong(), symmetrySettings, "cliffRockMask", true);
        fieldStoneMask = new BinaryMask(1, random.nextLong(), symmetrySettings, "fieldStoneMask", true);
        noProps = new BinaryMask(1, random.nextLong(), symmetrySettings, "noProps");
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

        cliffRockMask.randomize((reclaimDensity * .75f + random.nextFloat() * .25f) * .5f).setSize(mapSize + 1);
        cliffRockMask.intersect(impassable).dilute(.5f, SymmetryType.SPAWN, 6).minus(impassable).intersect(passableLand);
        fieldStoneMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .0025f).setSize(mapSize + 1);
        fieldStoneMask.intersect(passableLand).fillEdge(10, false);
        treeMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .15f).setSize(mapSize / 4);
        treeMask.inflate(2).erode(.5f, SymmetryType.SPAWN).erode(.5f, SymmetryType.SPAWN);
        treeMask.setSize(mapSize + 1);
        treeMask.intersect(passableLand.copy().deflate(8)).fillEdge(8, false);
    }


    protected void generatePropExclusionMasks() {
        noProps.init(unbuildable.getFinalMask());

        generateExclusionZones(noProps, 30, 2, 8);
    }

    protected void generateExclusionZones(BinaryMask mask, float spawnSpacing, float mexSpacing, float hydroSpacing) {
        map.getSpawns().forEach(spawn -> mask.fillCircle(spawn.getPosition(), spawnSpacing, true));
        map.getMexes().forEach(mex -> mask.fillCircle(mex.getPosition(), mexSpacing, true));
        map.getHydros().forEach(hydro -> mask.fillCircle(hydro.getPosition(), hydroSpacing, true));
    }

    @Override
    public void placeProps() {
        generatePropExclusionMasks();
        placePropsWithExclusion();
    }

    public void placePropsWithExclusion() {
        Pipeline.await(treeMask, cliffRockMask, fieldStoneMask);
        Util.timedRun("neroxis.generator", "placeProps", () -> {
            Biome biome = mapParameters.getBiome();
            propPlacer.placeProps(((BinaryMask) treeMask.getFinalMask()).minus(noProps), biome.getPropMaterials().getTreeGroups(), 3f, 7f);
            propPlacer.placeProps(cliffRockMask.getFinalMask(), biome.getPropMaterials().getRocks(), .5f, 2.5f);
            propPlacer.placeProps(((BinaryMask) fieldStoneMask.getFinalMask()).minus(noProps), biome.getPropMaterials().getBoulders(), 30f);
        });
    }

    @Override
    public void placeUnits() {
    }
}
