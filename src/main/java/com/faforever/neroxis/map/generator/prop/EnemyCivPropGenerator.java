package com.faforever.neroxis.map.generator.prop;

import com.faforever.neroxis.map.*;
import com.faforever.neroxis.map.generator.placement.UnitPlacer;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.mask.BooleanMask;
import com.faforever.neroxis.util.Pipeline;
import com.faforever.neroxis.util.Util;

import java.io.IOException;
import java.util.ArrayList;

public class EnemyCivPropGenerator extends BasicPropGenerator {

    protected BooleanMask baseMask;
    protected BooleanMask noBases;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        baseMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "baseMask", true);
        noBases = new BooleanMask(1, random.nextLong(), symmetrySettings);
    }

    @Override
    public void setupPipeline() {
        super.setupPipeline();
        setupCivilianPipeline();
    }

    protected void setupCivilianPipeline() {
        int mapSize = map.getSize();
        baseMask.setSize(mapSize / 4);

        if (!map.isUnexplored()) {
            baseMask.randomize(.005f).setSize(mapSize + 1);
            baseMask.multiply(passableLand.copy().subtract(unbuildable).deflate(24)).fillCenter(32, false).fillEdge(32, false);
        }
    }

    protected void generatePropExclusionMasks() {
        super.generatePropExclusionMasks();
        noProps.add(baseMask.getFinalMask());
    }

    protected void generateUnitExclusionMasks() {
        noBases.init(unbuildable.getFinalMask());
        noBases.inflate(12);
        generateExclusionZones(noBases, 128, 32, 32);
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(baseMask);
            Util.timedRun("com.faforever.neroxis.map.generator", "placeBases", () -> {
                Army army17 = new Army("ARMY_17", new ArrayList<>());
                Group army17Initial = new Group("INITIAL", new ArrayList<>());
                army17.addGroup(army17Initial);
                map.addArmy(army17);
                try {
                    unitPlacer.placeBases(baseMask.getFinalMask().subtract(noBases), UnitPlacer.MEDIUM_ENEMY, army17, army17Initial, 512f);
                } catch (IOException e) {
                    System.out.println("Could not generate bases due to lua parsing error");
                    e.printStackTrace();
                }
            });
        }
    }
}
