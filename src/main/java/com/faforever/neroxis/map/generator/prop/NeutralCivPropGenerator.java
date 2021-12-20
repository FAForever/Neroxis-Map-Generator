package com.faforever.neroxis.map.generator.prop;

import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.MapParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.generator.placement.UnitPlacer;
import com.faforever.neroxis.map.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtils;
import com.faforever.neroxis.util.Pipeline;

import java.io.IOException;
import java.util.ArrayList;

public strictfp class NeutralCivPropGenerator extends BasicPropGenerator {

    protected BooleanMask civReclaimMask;
    protected BooleanMask noCivs;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        civReclaimMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "civReclaimMask", true);

        noCivs = new BooleanMask(1, random.nextLong(), symmetrySettings);
    }

    @Override
    public void setupPipeline() {
        super.setupPipeline();
        setupCivilianPipeline();
    }

    protected void setupCivilianPipeline() {
        int mapSize = map.getSize();
        civReclaimMask.setSize(mapSize / 4);

        if (!map.isUnexplored()) {
            civReclaimMask.randomize(.005f).setSize(mapSize + 1);
            civReclaimMask.multiply(passableLand.copy().subtract(unbuildable).deflate(24)).fillCenter(32, false).fillEdge(64, false);
        } else {
            civReclaimMask.setSize(mapSize + 1);
        }
    }

    @Override
    protected void generatePropExclusionMasks() {
        super.generatePropExclusionMasks();
        noProps.add(civReclaimMask.getFinalMask());
    }

    protected void generateUnitExclusionMasks() {
        noCivs.init(unbuildable.getFinalMask());
        noCivs.inflate(12);
        generateExclusionZones(noCivs, 96, 32, 32);
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(civReclaimMask);
            DebugUtils.timedRun("com.faforever.neroxis.map.generator", "placeCivs", () -> {
                Army civilian = new Army("NEUTRAL_CIVILIAN", new ArrayList<>());
                Group civilianInitial = new Group("INITIAL", new ArrayList<>());
                civilian.addGroup(civilianInitial);
                map.addArmy(civilian);
                try {
                    unitPlacer.placeBases(civReclaimMask.getFinalMask().subtract(noCivs), UnitPlacer.MEDIUM_RECLAIM, civilian, civilianInitial, 256f);
                } catch (IOException e) {
                    System.out.println("Could not generate bases due to lua parsing error");
                    e.printStackTrace();
                }
            });
        }
    }
}
