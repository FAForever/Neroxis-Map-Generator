package com.faforever.neroxis.map.generator.prop;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtils;
import com.faforever.neroxis.util.Pipeline;

public strictfp class BasicPropGenerator extends PropGenerator {

    protected BooleanMask treeMask;
    protected BooleanMask cliffRockMask;
    protected BooleanMask fieldStoneMask;
    protected BooleanMask noProps;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        treeMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "treeMask", true);
        cliffRockMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "cliffRockMask", true);
        fieldStoneMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "fieldStoneMask", true);
        noProps = new BooleanMask(1, random.nextLong(), symmetrySettings, "noProps");
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
        cliffRockMask.multiply(impassable).dilute(.5f, 6).subtract(impassable).multiply(passableLand);
        fieldStoneMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .0025f).setSize(mapSize + 1);
        fieldStoneMask.multiply(passableLand).fillEdge(10, false);
        treeMask.randomize((reclaimDensity + random.nextFloat()) / 2f * .15f).setSize(mapSize / 4);
        treeMask.inflate(2).erode(.5f);
        treeMask.setSize(mapSize + 1);
        treeMask.multiply(passableLand.copy().deflate(8)).fillEdge(8, false);
    }


    protected void generatePropExclusionMasks() {
        noProps.init(unbuildable.getFinalMask());

        generateExclusionZones(noProps, 30, 2, 8);
    }

    protected void generateExclusionZones(BooleanMask mask, float spawnSpacing, float mexSpacing, float hydroSpacing) {
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
        DebugUtils.timedRun("com.faforever.neroxis.map.generator", "placeProps", () -> {
            Biome biome = mapParameters.getBiome();
            propPlacer.placeProps(treeMask.getFinalMask().subtract(noProps), biome.getPropMaterials().getTreeGroups(), 3f, 7f);
            propPlacer.placeProps(cliffRockMask.getFinalMask(), biome.getPropMaterials().getRocks(), .5f, 2.5f);
            propPlacer.placeProps(fieldStoneMask.getFinalMask().subtract(noProps), biome.getPropMaterials().getBoulders(), 30f);
        });
    }

    @Override
    public void placeUnits() {
    }
}
